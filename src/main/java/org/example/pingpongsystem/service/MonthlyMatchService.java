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


    /**
     * 1. 顶层赛程安排入口：按组别拆分报名者，创建小组并触发组内全循环赛程
     * 核心修改：确保分组逻辑正确，传递必要参数给子函数
     */
    private void arrangeMatchSchedule(Long matchId) {
        // 遍历所有组别类型（如男子组、女子组等，根据GroupType枚举动态适配）
        for (MatchRegistrationEntity.GroupType groupType : MatchRegistrationEntity.GroupType.values()) {
            // 1. 获取当前月赛、当前组别的有效报名记录
            List<MatchRegistrationEntity> registrations = registrationRepository
                    .findByMonthlyMatchIdAndGroupType(matchId, groupType);
            if (registrations.isEmpty()) {
                continue; // 该组别无报名，跳过处理
            }

            // 2. 获取所有可用球台（无球台时抛异常，确保赛程可执行）
            List<TableEntity> availableTables = tableRepository.findAll();
            if (availableTables.isEmpty()) {
                throw new RuntimeException("月赛ID：" + matchId + "，组别：" + groupType + " - 无可用球台，无法安排比赛");
            }

            // 3. 将报名者拆分为「每组最多6人」的子组（超过6人时分更小的组）
            List<List<MatchRegistrationEntity>> subgroups = splitIntoSubgroups(registrations);

            // 4. 为每个子组创建小组记录，并安排组内全循环比赛
            for (int i = 0; i < subgroups.size(); i++) {
                List<MatchRegistrationEntity> currentSubgroup = subgroups.get(i);

                // 4.1 保存小组信息（关联月赛、组别、小组编号、实际人数）
                MatchGroupEntity group = new MatchGroupEntity();
                group.setMonthlyMatchId(matchId);
                group.setGroupType(groupType);
                group.setSubgroupNumber(i + 1); // 小组编号从1开始（用户易理解）
                group.setSize(currentSubgroup.size()); // 记录真实参赛人数（不含虚拟轮空）
                MatchGroupEntity savedGroup = groupRepository.save(group);

                // 4.2 触发当前子组的全循环赛程安排
                arrangeSubgroupMatches(savedGroup.getId(), currentSubgroup, availableTables);
            }
        }
    }

    /**
     * 2. 报名者分组：将报名列表拆分为「每组最多6人」的子组，支持随机打乱
     * 核心修改：将subList转为独立ArrayList，避免原列表修改导致的视图异常
     */
    private List<List<MatchRegistrationEntity>> splitIntoSubgroups(List<MatchRegistrationEntity> registrations) {
        List<List<MatchRegistrationEntity>> subgroups = new ArrayList<>();
        final int MAX_GROUP_SIZE = 6; // 每组最多6人（需求明确）
        int totalRegistrants = registrations.size();

        // 随机打乱报名顺序（确保分组公平性，避免固定顺序影响对阵）
        Collections.shuffle(registrations);

        // 拆分逻辑：从0开始，每MAX_GROUP_SIZE人一组，最后一组不足6人也保留
        for (int i = 0; i < totalRegistrants; i += MAX_GROUP_SIZE) {
            // 计算当前组的结束索引（避免超出列表长度）
            int endIndex = Math.min(i + MAX_GROUP_SIZE, totalRegistrants);
            // 关键修改：用new ArrayList()将subList转为独立列表（原subList依赖原列表，易出问题）
            List<MatchRegistrationEntity> subgroup = new ArrayList<>(registrations.subList(i, endIndex));
            subgroups.add(subgroup);
        }

        return subgroups;
    }

    /**
     * 3. 单场比赛创建：封装比赛基本信息，关联小组、轮次、选手、球台
     * 核心修改：增强异常提示，明确参数来源，确保轮空时player2Id为null
     */
    private MatchScheduleEntity createMatchSchedule(Long groupId, int round, Long player1Id, Long player2Id, List<TableEntity> tables) {
        // 通过小组ID查询关联的月赛ID（确保数据一致性）
        MatchGroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("小组不存在，小组ID：" + groupId));

        MatchScheduleEntity schedule = new MatchScheduleEntity();
        schedule.setMonthlyMatchId(group.getMonthlyMatchId()); // 关联月赛（从小组间接获取，避免直接传参错误）
        schedule.setGroupId(groupId); // 关联当前小组
        schedule.setRoundNumber(round); // 轮次（从循环参数传入）
        schedule.setPlayer1Id(player1Id); // 选手1（轮空时不为null）
        schedule.setPlayer2Id(player2Id); // 选手2（轮空时为null，符合实体定义）
        // 随机分配球台：从可用球台中随机选一个（确保球台资源有效）
        schedule.setTableId(tables.get(new Random().nextInt(tables.size())).getId());
        schedule.setResult(MatchScheduleEntity.MatchResult.NOT_STARTED); // 初始状态：未开始

        return schedule;
    }

    /**
     * 4. 组内全循环赛程：核心逻辑，实现「固定1号+顺时针轮转」+ 奇数轮空优化
     * 核心修改：重构对阵逻辑，用虚拟轮空统一奇偶处理，确保匹配用户示例
     */
    private void arrangeSubgroupMatches(Long groupId, List<MatchRegistrationEntity> subgroup, List<TableEntity> tables) {
        // 步骤1：提取当前小组的真实选手ID（排除轮空，仅保留实际报名学员）
        List<Long> realPlayerIds = subgroup.stream()
                .map(MatchRegistrationEntity::getStudentId)
                .collect(Collectors.toList());
        int realPlayerCount = realPlayerIds.size();
        if (realPlayerCount < 2) {
            return; // 不足2人，无法安排比赛（含轮空也至少需2个"名额"）
        }

        // 步骤2：奇数人数处理：添加虚拟轮空标记（-1L），统一按偶数逻辑计算
        List<Long> extendedPlayerIds = new ArrayList<>(realPlayerIds);
        if (realPlayerCount % 2 != 0) {
            extendedPlayerIds.add(-1L); // -1L为虚拟轮空，后续替换为null
        }
        int extendedSize = extendedPlayerIds.size(); // 扩展后的人数（必为偶数）
        int totalRounds = extendedSize - 1; // 全循环轮次：人数-1（如6人5轮，5人+轮空6人也5轮）

        // 步骤3：初始化当前轮选手列表（从初始扩展列表开始）
        List<Long> currentRoundPlayers = new ArrayList<>(extendedPlayerIds);

        // 步骤4：循环安排每一轮比赛
        for (int round = 1; round <= totalRounds; round++) {
            List<MatchScheduleEntity> roundMatches = new ArrayList<>();

            // 4.1 生成当前轮对阵：从列表两端向中间配对（0↔n-1，1↔n-2，...）
            for (int i = 0; i < extendedSize / 2; i++) {
                int opponentIndex = extendedSize - 1 - i;
                Long player1 = currentRoundPlayers.get(i);
                Long player2 = currentRoundPlayers.get(opponentIndex);

                // 4.2 虚拟轮空替换：将-1L转为null（符合MatchScheduleEntity的轮空定义）
                player1 = (player1 != null && player1.equals(-1L)) ? null : player1;
                player2 = (player2 != null && player2.equals(-1L)) ? null : player2;

                // 4.3 轮空逻辑优化：确保player1不为null（轮空选手作为player1，player2为null）
                if (player1 == null) {
                    // 交换选手顺序，避免player1为null（实体中player1可非空，player2可空）
                    Long temp = player1;
                    player1 = player2;
                    player2 = temp;
                }

                // 4.4 无效配对过滤（理论上不会触发，双重保障）
                if (player1 == null) {
                    continue;
                }

                // 4.5 创建单场比赛并加入当前轮列表
                MatchScheduleEntity match = createMatchSchedule(
                        groupId, round, player1, player2, tables);
                roundMatches.add(match);
            }

            // 4.6 保存当前轮比赛（批量保存，提升效率）
            if (!roundMatches.isEmpty()) {
                scheduleRepository.saveAll(roundMatches);
            }

            // 4.7 顺时针轮转：固定第1位，将最后1位移到第2位（核心轮转逻辑）
            if (extendedSize > 2) { // 仅当人数>2时需要轮转（人数=2时1轮即可，无需轮转）
                Long lastPlayer = currentRoundPlayers.remove(extendedSize - 1); // 移除最后一位
                currentRoundPlayers.add(1, lastPlayer); // 插入到第2位（索引1）
            }
        }
    }
}
