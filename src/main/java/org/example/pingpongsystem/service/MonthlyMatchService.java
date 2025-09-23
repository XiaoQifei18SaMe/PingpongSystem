package org.example.pingpongsystem.service;

import lombok.RequiredArgsConstructor;
import org.example.pingpongsystem.entity.*;
import org.example.pingpongsystem.repository.*;
import org.example.pingpongsystem.utility.Result;
import org.example.pingpongsystem.utility.StatusCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonthlyMatchService {

    private final MonthlyMatchRepository matchRepository;
    private final MatchRegistrationRepository registrationRepository;
    private final MatchGroupRepository groupRepository;
    private final MatchScheduleRepository scheduleRepository;
    private final StudentAccountRepository studentAccountRepository;
    private final PaymentService paymentService;
    private final PaymentRecordRepository paymentRecordRepository;
    private final TableRepository tableRepository;

    // 定时任务触发后创建比赛，比赛开始时间为触发后5分钟
    @Scheduled(cron = "0 19 13 23 9 ?") // 触发时间：2025年9月23日10:16
    @Transactional
    public void createMonthlyMatch() {
        // 获取定时任务触发时的当前时间（包含时分秒）
        LocalDateTime now = LocalDateTime.now();

        // 比赛开始时间 = 定时任务触发时间 + 5分钟
        LocalDateTime matchTime = now.plusMinutes(5);

        // 截止时间 = 比赛开始时间 - 1分钟
        LocalDateTime deadline = matchTime.minusMinutes(1);

        MonthlyMatchEntity match = new MonthlyMatchEntity();
        match.setTitle(now.getYear() + "年" + now.getMonthValue() + "月乒乓球月赛");
        match.setStartTime(matchTime); // 设置比赛开始时间
        match.setRegistrationDeadline(deadline); // 设置截止时间
        match.setYear(now.getYear());
        match.setMonth(now.getMonthValue());
        match.setStatus(MonthlyMatchEntity.MatchStatus.NOT_STARTED);

        matchRepository.save(match);
    }

    // 每月1日自动创建当月比赛
//    @Scheduled(cron = "0 0 0 1 * ?")
//    @Transactional
//    public void createMonthlyMatch() {
//        LocalDate now = LocalDate.now();
//        // 找到当月第四个星期天
//        LocalDate fourthSunday = now.with(TemporalAdjusters.dayOfWeekInMonth(4, DayOfWeek.SUNDAY));
//        LocalDateTime matchTime = fourthSunday.atTime(9, 0); // 上午9点开始
//        LocalDateTime deadline = matchTime.minusWeeks(1); // 提前7天截止
//
//        MonthlyMatchEntity match = new MonthlyMatchEntity();
//        match.setTitle(now.getYear() + "年" + now.getMonthValue() + "月乒乓球月赛");
//        match.setStartTime(matchTime);
//        match.setRegistrationDeadline(deadline);
//        match.setYear(now.getYear());
//        match.setMonth(now.getMonthValue());
//        match.setStatus(MonthlyMatchEntity.MatchStatus.NOT_STARTED);
//
//        matchRepository.save(match);
//    }

    // 管理员修改比赛时间
    @Transactional
    public Result<MonthlyMatchEntity> updateMatchTime(Long matchId, LocalDateTime startTime) {
        Optional<MonthlyMatchEntity> matchOpt = matchRepository.findById(matchId);
        if (matchOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "月赛不存在");
        }

        MonthlyMatchEntity match = matchOpt.get();
        // 确保修改时间在报名开始前
        if (match.getStatus() != MonthlyMatchEntity.MatchStatus.NOT_STARTED) {
            return Result.error(StatusCode.FAIL, "只能在报名开始前修改比赛时间");
        }

        match.setStartTime(startTime);
        match.setRegistrationDeadline(startTime.minusWeeks(1));
        matchRepository.save(match);
        return Result.success(match);
    }

    // 学员报名月赛
    @Transactional
    public Result<MatchRegistrationEntity> registerForMatch(Long matchId, Long studentId, MatchRegistrationEntity.GroupType groupType) {
        Optional<MonthlyMatchEntity> matchOpt = matchRepository.findById(matchId);
        if (matchOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "月赛不存在");
        }

        MonthlyMatchEntity match = matchOpt.get();
        // 检查报名时间是否有效
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(match.getStartTime().minusMonths(1)) || now.isAfter(match.getRegistrationDeadline())) {
            return Result.error(StatusCode.FAIL, "不在报名时间范围内");
        }

        // 检查是否已报名
        Optional<MatchRegistrationEntity> existingReg = registrationRepository.findByMonthlyMatchIdAndStudentId(matchId, studentId);
        if (existingReg.isPresent()) {
            return Result.error(StatusCode.FAIL, "您已报名该月赛");
        }

        // 检查余额
        Optional<StudentAccountEntity> accountOpt = studentAccountRepository.findByStudentId(studentId);
        if (accountOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "学员账户不存在");
        }

        StudentAccountEntity account = accountOpt.get();
        double fee = 30.0; // 报名费30元
        if (account.getBalance() < fee) {
            return Result.error(StatusCode.FAIL, "余额不足，请先充值");
        }

        // 创建支付记录并扣减余额
        PaymentRecordEntity paymentRecord = paymentService.createMatchPaymentRecord(studentId, fee);
        account.setBalance(account.getBalance() - fee);
        studentAccountRepository.save(account);

        // 创建报名记录
        MatchRegistrationEntity registration = new MatchRegistrationEntity();
        registration.setMonthlyMatchId(matchId);
        registration.setStudentId(studentId);
        registration.setGroupType(groupType);
        registration.setPaid(true);
        registration.setPaymentRecordId(paymentRecord.getId());
        registration.setRegistrationTime(LocalDateTime.now());

        MatchRegistrationEntity savedReg = registrationRepository.save(registration);
        return Result.success(savedReg);
    }

    // 定时任务：报名截止后自动安排赛程
    //@Scheduled(cron = "0 0 0 * * ?") // 每天凌晨执行
    @Scheduled(cron = "0 */1 * * * ?")  // 每1分钟执行一次（测试用，可调整为每小时）
    @Transactional
    public void arrangeMatchesAutomatically() {
        LocalDateTime now = LocalDateTime.now();
        // 找到所有已截止报名但未安排赛程的比赛
        List<MonthlyMatchEntity> matches = matchRepository.findAll().stream()
                .filter(match -> match.getRegistrationDeadline().isBefore(now) &&
                        match.getStatus() == MonthlyMatchEntity.MatchStatus.REGISTERING)
                .collect(Collectors.toList());

        for (MonthlyMatchEntity match : matches) {
            arrangeMatchSchedule(match.getId());
            match.setStatus(MonthlyMatchEntity.MatchStatus.REGISTRATION_CLOSED);
            matchRepository.save(match);
        }
    }

    // 安排赛程
    private void arrangeMatchSchedule(Long matchId) {
        // 处理每个组别
        for (MatchRegistrationEntity.GroupType groupType : MatchRegistrationEntity.GroupType.values()) {
            List<MatchRegistrationEntity> registrations = registrationRepository
                    .findByMonthlyMatchIdAndGroupType(matchId, groupType);

            if (registrations.isEmpty()) continue;

            // 获取所有球台
            List<TableEntity> tables = tableRepository.findAll();
            if (tables.isEmpty()) {
                throw new RuntimeException("没有可用球台，无法安排比赛");
            }

            // 根据人数进行分组
            List<List<MatchRegistrationEntity>> subgroups = splitIntoSubgroups(registrations);

            // 为每个小组创建记录并安排赛程
            for (int i = 0; i < subgroups.size(); i++) {
                List<MatchRegistrationEntity> subgroup = subgroups.get(i);

                // 创建小组记录
                MatchGroupEntity group = new MatchGroupEntity();
                group.setMonthlyMatchId(matchId);
                group.setGroupType(groupType);
                group.setSubgroupNumber(i + 1);
                group.setSize(subgroup.size());
                MatchGroupEntity savedGroup = groupRepository.save(group);

                // 安排小组内的比赛
                arrangeSubgroupMatches(savedGroup.getId(), subgroup, tables);
            }
        }
    }

    // 将报名者分成小组（每组最多6人）
    private List<List<MatchRegistrationEntity>> splitIntoSubgroups(List<MatchRegistrationEntity> registrations) {
        List<List<MatchRegistrationEntity>> subgroups = new ArrayList<>();
        int groupSize = 6;
        int total = registrations.size();

        // 随机打乱顺序
        Collections.shuffle(registrations);

        for (int i = 0; i < total; i += groupSize) {
            int end = Math.min(i + groupSize, total);
            subgroups.add(registrations.subList(i, end));
        }

        return subgroups;
    }

    // 安排小组内的比赛
    private void arrangeSubgroupMatches(Long groupId, List<MatchRegistrationEntity> subgroup, List<TableEntity> tables) {
        int n = subgroup.size();
        if (n <= 1) return; // 不足2人无法比赛

        // 为每位选手分配编号
        Map<Integer, Long> playerNumbers = new HashMap<>();
        for (int i = 0; i < n; i++) {
            playerNumbers.put(i + 1, subgroup.get(i).getStudentId());
        }

        // 计算轮次
        int rounds = n % 2 == 0 ? n - 1 : n;

        // 安排每一轮比赛
        for (int round = 1; round <= rounds; round++) {
            List<MatchScheduleEntity> roundMatches = new ArrayList<>();

            // 根据奇偶人数安排不同的对阵
            if (n % 2 == 0) {
                // 偶数人
                for (int i = 1; i <= n / 2; i++) {
                    int opponent = n - i + 1;
                    if (i == 1 && round > 1) {
                        opponent = n - round + 2;
                    } else if (i > 1) {
                        opponent = (i + round - 2) % (n - 1) + 2;
                    }

                    MatchScheduleEntity schedule = createMatchSchedule(
                            groupId, round, playerNumbers.get(i), playerNumbers.get(opponent), tables);
                    roundMatches.add(schedule);
                }
            } else {
                // 奇数人（有轮空）
                for (int i = 1; i <= (n - 1) / 2; i++) {
                    int opponent;
                    if (i == 1) {
                        opponent = (round % (n - 1) == 0) ? n : round % (n - 1) + 1;
                        if (opponent == 1) opponent = n;
                    } else {
                        opponent = (i + round - 2) % (n - 1) + 2;
                        if (opponent == i) opponent = n;
                    }

                    MatchScheduleEntity schedule = createMatchSchedule(
                            groupId, round, playerNumbers.get(i), playerNumbers.get(opponent), tables);
                    roundMatches.add(schedule);
                }

                // 安排轮空选手
                int byePlayer = (round % (n - 1) == 0) ? 1 : round % (n - 1) + 1;
                MatchScheduleEntity byeSchedule = createMatchSchedule(
                        groupId, round, playerNumbers.get(byePlayer), null, tables);
                roundMatches.add(byeSchedule);
            }

            scheduleRepository.saveAll(roundMatches);
        }
    }

    // 创建单场比赛安排
    private MatchScheduleEntity createMatchSchedule(Long groupId, int round, Long player1Id, Long player2Id, List<TableEntity> tables) {
        MatchScheduleEntity schedule = new MatchScheduleEntity();
        // 从月赛ID中提取（实际应该通过groupId查询获得）
        Long matchId = groupRepository.findById(groupId).orElseThrow().getMonthlyMatchId();

        schedule.setMonthlyMatchId(matchId);
        schedule.setGroupId(groupId);
        schedule.setRoundNumber(round);
        schedule.setPlayer1Id(player1Id);
        schedule.setPlayer2Id(player2Id);
        // 随机分配球台
        schedule.setTableId(tables.get(new Random().nextInt(tables.size())).getId());
        schedule.setResult(MatchScheduleEntity.MatchResult.NOT_STARTED);

        return schedule;
    }

    // 获取学员的报名记录
    public Result<List<MatchRegistrationEntity>> getStudentRegistrations(Long studentId) {
        return Result.success(registrationRepository.findByStudentId(studentId));
    }

    // 获取月赛的所有组别报名情况
    public Result<Map<MatchRegistrationEntity.GroupType, Long>> getGroupRegistrationCounts(Long matchId) {
        Map<MatchRegistrationEntity.GroupType, Long> counts = new EnumMap<>(MatchRegistrationEntity.GroupType.class);
        for (MatchRegistrationEntity.GroupType groupType : MatchRegistrationEntity.GroupType.values()) {
            counts.put(groupType, registrationRepository.countByMonthlyMatchIdAndGroupType(matchId, groupType));
        }
        return Result.success(counts);
    }

    // 获取学员的比赛安排
    public Result<List<MatchScheduleEntity>> getStudentMatchSchedule(Long matchId, Long studentId) {
        // 检查报名是否截止
        Optional<MonthlyMatchEntity> matchOpt = matchRepository.findById(matchId);
        if (matchOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "月赛不存在");
        }

        MonthlyMatchEntity match = matchOpt.get();
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(match.getRegistrationDeadline())) {
            return Result.error(StatusCode.FAIL, "报名尚未截止，暂不能查看赛程");
        }

        return Result.success(scheduleRepository.findByMonthlyMatchIdAndPlayer1IdOrPlayer2Id(matchId, studentId, studentId));
    }

    // 获取当前可报名的月赛
    public Result<List<MonthlyMatchEntity>> getAvailableMatches() {
        LocalDateTime now = LocalDateTime.now();
        List<MonthlyMatchEntity> matches = matchRepository.findAll().stream()
                .filter(match -> now.isAfter(match.getStartTime().minusMonths(1)) &&
                        now.isBefore(match.getRegistrationDeadline()) &&
                        match.getStatus() == MonthlyMatchEntity.MatchStatus.REGISTERING)
                .collect(Collectors.toList());
        return Result.success(matches);
    }

    /**
     * 定时任务：检查未开始的比赛，自动切换到报名中（或直接截止并安排赛程）
     * 执行频率：每1分钟（测试用，正式环境可改为每10分钟/每小时，根据需求调整）
     */
    @Scheduled(cron = "0 */1 * * * ?")
    @Transactional
    public void checkAndSwitchMatchStatus() {
        LocalDateTime now = LocalDateTime.now();
        // 1. 查询所有“未开始”状态的比赛
        List<MonthlyMatchEntity> notStartedMatches = matchRepository.findByStatus(MonthlyMatchEntity.MatchStatus.NOT_STARTED);

        for (MonthlyMatchEntity match : notStartedMatches) {
            // 计算“报名开始时间”（原代码 registerForMatch 中定义为“比赛开始前1个月”）
            LocalDateTime registrationStartTime = match.getStartTime().minusMonths(1);
            // 报名截止时间（已在创建比赛时设置）
            LocalDateTime registrationDeadline = match.getRegistrationDeadline();

            // 2. 状态流转判断
            if (now.isAfter(registrationStartTime) || now.isEqual(registrationStartTime)) {
                // 2.1 若当前时间在“报名开始后 + 截止前”：切换为【报名中】
                if (now.isBefore(registrationDeadline)) {
                    match.setStatus(MonthlyMatchEntity.MatchStatus.REGISTERING);
                    matchRepository.save(match);
                }
                // 2.2 若当前时间已过“报名截止时间”：直接切换为【报名截止】并安排赛程
                else if (now.isAfter(registrationDeadline) || now.isEqual(registrationDeadline)) {
                    match.setStatus(MonthlyMatchEntity.MatchStatus.REGISTRATION_CLOSED);
                    matchRepository.save(match);
                    // 立即安排赛程（避免遗漏）
                    arrangeMatchSchedule(match.getId());
                }
            }
        }
    }

    /**
     * 根据ID查询单个月赛详情（用于前端报名记录关联月赛信息）
     */
    public Result<MonthlyMatchEntity> getMatchById(Long matchId) {
        Optional<MonthlyMatchEntity> matchOpt = matchRepository.findById(matchId);
        if (matchOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "月赛不存在");
        }
        return Result.success(matchOpt.get());
    }
}
