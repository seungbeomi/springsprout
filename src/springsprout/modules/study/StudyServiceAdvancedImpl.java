package springsprout.modules.study;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import springsprout.common.exception.AsyncExceptionTemplate;
import springsprout.common.exception.ExceptionTemplate;
import springsprout.common.exception.ExceptionalWork;
import springsprout.domain.Study;
import springsprout.domain.enumeration.StudyStatus;
import springsprout.modules.calendar.GoogleCalendarService;
import springsprout.modules.member.MemberRepository;
import springsprout.modules.study.support.StudyContainer;
import springsprout.modules.study.support.StudyCriteria;
import springsprout.modules.study.support.StudyIndexInfo;
import springsprout.service.notification.NotificationService;
import springsprout.service.notification.message.StudyMailMessage;
import springsprout.service.security.SecurityService;

import javax.annotation.Resource;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: whiteship2000
 * Date: 2010. 11. 18
 * Time: 오후 11:03:28
 */
@Service("advancedStudyService")
public class StudyServiceAdvancedImpl implements StudyService {

    @Resource StudyService studyService;
    @Resource NotificationService unifiedNotificationService;
    @Resource ThreadPoolTaskExecutor myExecutor;
    
    @Autowired GoogleCalendarService calendarService;
    @Autowired MemberRepository memberRepository;
    @Autowired SecurityService securityService;

    @Autowired AsyncExceptionTemplate exceptionTemplate;

    public void addStudy(final Study study) {
        studyService.addStudy(study);

        exceptionTemplate.catchAll(new ExceptionalWork(){
            public void run() throws Exception {
                unifiedNotificationService.sendMessage(new StudyMailMessage(study, StudyStatus.OPEN, memberRepository.getMemberList()));
                calendarService.createNewStudyCalendar(study);
            }
        });
    }

    public void updateStudy(final Study study, final Boolean isGoingToBeNotified) {
        studyService.updateStudy(study, isGoingToBeNotified);

        exceptionTemplate.catchAll(new ExceptionalWork(){
            public void run() throws Exception {
                calendarService.synchronizeForLegacy(study);
                calendarService.synchronizeForLegacy(study.getMeetings());
                calendarService.updateStudyCalendar(study);
                calendarService.addToAccessControlList(study, securityService.getCurrentMember());

                if (isGoingToBeNotified && (study.getStatus() != StudyStatus.ENDED)) {
                    unifiedNotificationService.sendMessage(new StudyMailMessage(study, StudyStatus.UPDATED, memberRepository.getMemberList()));
                }
            }
        });
    }

    public List<Study> findActiveStudies() {
        return studyService.findActiveStudies();
    }

    public List<Study> findActiveStudies(int rows) {
        return studyService.findActiveStudies(rows);
    }

    public List<Study> findPastStudies() {
        return studyService.findActiveStudies();
    }

    public StudyContainer findStudies(StudyCriteria cri) {
        return studyService.findStudies(cri);
    }

    public Study getStudyById(int id) {
        return studyService.getStudyById(id);
    }

    public void notify(int studyId) {
        studyService.notify(studyId);
    }

    public void deleteStudy(final Study study) {
        exceptionTemplate.catchAll(new ExceptionalWork(){
            public void run() throws Exception {
                calendarService.deleteStudyCalendar(study);
            }
        });

        studyService.deleteStudy(study);
    }

    public void endStudy(Study study) {
        studyService.endStudy(study);
    }

    public void startStudy(Study study) {
        studyService.startStudy(study);
    }

    public void addCurrentMember(final Study study) {
        studyService.addCurrentMember(study);

        exceptionTemplate.catchAll(new ExceptionalWork(){
            public void run() throws Exception {
                calendarService.addToAccessControlList(study, securityService.getPersistentMember());
            }
        });
    }

    public void removeCurrentMember(final Study study) {
        studyService.removeCurrentMember(study);

        exceptionTemplate.catchAll(new ExceptionalWork(){
            public void run() throws Exception {
                calendarService.removeToAccessControlList(study, securityService.getPersistentMember());
            }
        });
    }

    public boolean isCurrentUserAlreadyJoinedIn(int studyId) {
        return studyService.isCurrentUserAlreadyJoinedIn(studyId);
    }

    public boolean isCurrentUserTheStudyManagerOrAdmin(int studyId) {
        return studyService.isCurrentUserTheStudyManagerOrAdmin(studyId);
    }

    public StudyIndexInfo makeStudyIndexInfo() {
        return studyService.makeStudyIndexInfo();
    }
}
