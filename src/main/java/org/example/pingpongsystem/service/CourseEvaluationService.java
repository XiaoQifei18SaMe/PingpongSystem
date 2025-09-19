package org.example.pingpongsystem.service;

import lombok.RequiredArgsConstructor;
import org.example.pingpongsystem.entity.CourseAppointmentEntity;
import org.example.pingpongsystem.entity.CourseEvaluationEntity;
import org.example.pingpongsystem.entity.NotificationEntity;
import org.example.pingpongsystem.repository.CourseAppointmentRepository;
import org.example.pingpongsystem.repository.CourseEvaluationRepository;
import org.example.pingpongsystem.utility.Result;
import org.example.pingpongsystem.utility.StatusCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CourseEvaluationService {

    private final CourseEvaluationRepository evaluationRepository;
    private final CourseAppointmentRepository appointmentRepository;
    private final NotificationService notificationService;

    /**
     * 定时任务：检查并处理已结束的课程
     * 每小时执行一次
     */
    //@Scheduled(cron = "0 0 * * * ?")
    /**
     * 定时任务：检查并处理已结束的课程
     * 每10分钟执行一次
     */
//    @Scheduled(cron = "0 */10 * * * ?")  // 修改cron表达式为每10分钟
    @Scheduled(cron = "0 */1 * * * ?")  // 每1分钟执行一次
    @Transactional
    public void processCompletedCourses() {
        LocalDateTime now = LocalDateTime.now();
        // 查询所有已确认但未完成，且结束时间已过的课程
        List<CourseAppointmentEntity> completedCourses = appointmentRepository.findAll().stream()
                .filter(appointment -> appointment.getStatus() == CourseAppointmentEntity.AppointmentStatus.CONFIRMED
                        && appointment.getEndTime().isBefore(now))
                .toList();

        // 更新课程状态为已完成并发送评价通知
        for (CourseAppointmentEntity course : completedCourses) {
            course.setStatus(CourseAppointmentEntity.AppointmentStatus.COMPLETED);
            appointmentRepository.save(course);

            // 发送学员评价通知
            notificationService.createNotification(
                    course.getStudentId(),
                    NotificationEntity.UserType.STUDENT,
                    course.getId(),
                    "您有一节课程已完成，请对教练进行评价"
            );

            // 发送教练评价通知
            notificationService.createNotification(
                    course.getCoachId(),
                    NotificationEntity.UserType.COACH,
                    course.getId(),
                    "您有一节课程已完成，请对学员进行评价"
            );
        }
    }


    /**
     * 提交评价
     */
    @Transactional
    public Result<CourseEvaluationEntity> submitEvaluation(
            Long appointmentId, Long evaluatorId,
            CourseEvaluationEntity.EvaluatorType evaluatorType, String content) {

        // 验证预约是否存在且已完成
        Optional<CourseAppointmentEntity> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "课程预约不存在");
        }

        CourseAppointmentEntity appointment = appointmentOpt.get();
        if (appointment.getStatus() != CourseAppointmentEntity.AppointmentStatus.COMPLETED) {
            return Result.error(StatusCode.FAIL, "只有已完成的课程才能评价");
        }

        // 验证评价人是否与课程相关
        if (evaluatorType == CourseEvaluationEntity.EvaluatorType.STUDENT
                && !appointment.getStudentId().equals(evaluatorId)) {
            return Result.error(StatusCode.FAIL, "您不是该课程的学员，无法评价");
        }

        if (evaluatorType == CourseEvaluationEntity.EvaluatorType.COACH
                && !appointment.getCoachId().equals(evaluatorId)) {
            return Result.error(StatusCode.FAIL, "您不是该课程的教练，无法评价");
        }

        // 检查是否已评价
        List<CourseEvaluationEntity> existingEvaluations = evaluationRepository
                .findByAppointmentId(appointmentId);
        boolean alreadyEvaluated = existingEvaluations.stream()
                .anyMatch(e -> e.getEvaluatorId().equals(evaluatorId)
                        && e.getEvaluatorType() == evaluatorType);

        if (alreadyEvaluated) {
            return Result.error(StatusCode.FAIL, "您已评价过该课程");
        }

        // 创建评价记录
        CourseEvaluationEntity evaluation = new CourseEvaluationEntity();
        evaluation.setAppointmentId(appointmentId);
        evaluation.setEvaluatorId(evaluatorId);
        evaluation.setEvaluatorType(evaluatorType);
        evaluation.setContent(content);
        evaluation.setCreateTime(LocalDateTime.now());

        CourseEvaluationEntity savedEvaluation = evaluationRepository.save(evaluation);
        return Result.success(savedEvaluation);
    }

    /**
     * 获取课程的所有评价
     */
    public Result<List<CourseEvaluationEntity>> getEvaluationsByAppointment(Long appointmentId) {
        List<CourseEvaluationEntity> evaluations = evaluationRepository.findByAppointmentId(appointmentId);
        return Result.success(evaluations);
    }

    /**
     * 获取用户的所有评价记录
     */
    public Result<List<CourseEvaluationEntity>> getEvaluationsByUser(
            Long userId, CourseEvaluationEntity.EvaluatorType type) {
        List<CourseEvaluationEntity> evaluations = evaluationRepository
                .findByEvaluatorIdAndEvaluatorType(userId, type);
        return Result.success(evaluations);
    }

}