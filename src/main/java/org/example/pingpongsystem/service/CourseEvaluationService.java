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
     * 定时任务：处理已结束课程（原逻辑不变）
     */
    @Scheduled(cron = "0 */1 * * * ?")  // 每1分钟执行一次（测试用，可调整为每小时）
    @Transactional
    public void processCompletedCourses() {
        LocalDateTime now = LocalDateTime.now();
        List<CourseAppointmentEntity> completedCourses = appointmentRepository.findAll().stream()
                .filter(appointment -> appointment.getStatus() == CourseAppointmentEntity.AppointmentStatus.CONFIRMED
                        && appointment.getEndTime().isBefore(now))
                .toList();

        for (CourseAppointmentEntity course : completedCourses) {
            course.setStatus(CourseAppointmentEntity.AppointmentStatus.COMPLETED);
            appointmentRepository.save(course);

            // 发送评价通知（原逻辑不变）
            notificationService.createNotification(
                    course.getStudentId(),
                    NotificationEntity.UserType.STUDENT,
                    course.getId(),
                    "您有一节课程已完成，请对教练进行评价"
            );
            notificationService.createNotification(
                    course.getCoachId(),
                    NotificationEntity.UserType.COACH,
                    course.getId(),
                    "您有一节课程已完成，请对学员进行评价"
            );
        }
    }

    /**
     * 新增评价（原submitEvaluation重命名）
     */
    @Transactional
    public Result<CourseEvaluationEntity> createEvaluation(
            Long appointmentId, Long evaluatorId,
            CourseEvaluationEntity.EvaluatorType evaluatorType, String content) {

        // 1. 校验预约存在且已完成
        Optional<CourseAppointmentEntity> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "课程预约不存在");
        }
        CourseAppointmentEntity appointment = appointmentOpt.get();
        if (appointment.getStatus() != CourseAppointmentEntity.AppointmentStatus.COMPLETED) {
            return Result.error(StatusCode.FAIL, "只有已完成的课程才能评价");
        }

        // 2. 校验评价人是否与课程相关
        boolean isStudentValid = evaluatorType == CourseEvaluationEntity.EvaluatorType.STUDENT
                && appointment.getStudentId().equals(evaluatorId);
        boolean isCoachValid = evaluatorType == CourseEvaluationEntity.EvaluatorType.COACH
                && appointment.getCoachId().equals(evaluatorId);
        if (!isStudentValid && !isCoachValid) {
            return Result.error(StatusCode.FAIL, "您无权限评价此课程");
        }

        // 3. 校验是否已评价
        List<CourseEvaluationEntity> existing = evaluationRepository.findByAppointmentId(appointmentId);
        boolean alreadyEvaluated = existing.stream()
                .anyMatch(e -> e.getEvaluatorId().equals(evaluatorId)
                        && e.getEvaluatorType() == evaluatorType);
        if (alreadyEvaluated) {
            return Result.error(StatusCode.FAIL, "您已评价过该课程");
        }

        // 4. 创建评价（时间由@PrePersist自动维护）
        CourseEvaluationEntity evaluation = new CourseEvaluationEntity();
        evaluation.setAppointmentId(appointmentId);
        evaluation.setEvaluatorId(evaluatorId);
        evaluation.setEvaluatorType(evaluatorType);
        evaluation.setContent(content);
        CourseEvaluationEntity saved = evaluationRepository.save(evaluation);

        return Result.success(saved);
    }

    /**
     * 获取课程的所有评价（原逻辑不变）
     */
    public Result<List<CourseEvaluationEntity>> getEvaluationsByAppointment(Long appointmentId) {
        List<CourseEvaluationEntity> evaluations = evaluationRepository.findByAppointmentId(appointmentId);
        return Result.success(evaluations);
    }

    /**
     * 获取用户的评价记录（原逻辑不变）
     */
    public Result<List<CourseEvaluationEntity>> getEvaluationsByUser(
            Long userId, CourseEvaluationEntity.EvaluatorType type) {
        List<CourseEvaluationEntity> evaluations = evaluationRepository
                .findByEvaluatorIdAndEvaluatorType(userId, type);
        return Result.success(evaluations);
    }

    /**
     * 编辑评价（新增核心逻辑）
     */
    @Transactional
    public Result<CourseEvaluationEntity> updateEvaluation(
            Long evaluationId, String content, Long evaluatorId) {

        // 1. 校验评价是否存在
        Optional<CourseEvaluationEntity> evaluationOpt = evaluationRepository.findById(evaluationId);
        if (evaluationOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "评价不存在或已删除");
        }
        CourseEvaluationEntity evaluation = evaluationOpt.get();

        // 2. 校验权限：只能编辑自己的评价
        if (!evaluation.getEvaluatorId().equals(evaluatorId)) {
            return Result.error(StatusCode.FAIL, "无权限修改他人评价");
        }

        // 3. 校验评价内容不为空
        if (content == null || content.trim().isEmpty()) {
            return Result.error(StatusCode.FAIL, "评价内容不能为空");
        }

        // 4. 更新内容（updateTime由@PreUpdate自动维护）
        evaluation.setContent(content);
        CourseEvaluationEntity updated = evaluationRepository.save(evaluation);

        return Result.success(updated);
    }

    /**
     * 删除评价（新增核心逻辑）
     */
    @Transactional
    public Result<Boolean> deleteEvaluation(Long evaluationId, Long evaluatorId) {

        // 1. 校验评价是否存在
        Optional<CourseEvaluationEntity> evaluationOpt = evaluationRepository.findById(evaluationId);
        if (evaluationOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "评价不存在或已删除");
        }
        CourseEvaluationEntity evaluation = evaluationOpt.get();

        // 2. 校验权限：只能删除自己的评价
        if (!evaluation.getEvaluatorId().equals(evaluatorId)) {
            return Result.error(StatusCode.FAIL, "无权限删除他人评价");
        }

        // 3. 执行删除
        evaluationRepository.delete(evaluation);
        return Result.success(true);
    }
}