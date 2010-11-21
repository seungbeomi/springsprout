package springsprout.modules.study;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import springsprout.domain.Study;
import springsprout.modules.study.exception.StudyMaximumOverException;
import springsprout.modules.study.meeting.support.CountInfoDTO;
import springsprout.service.security.SecurityService;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import static springsprout.common.SpringSprout2System.JSON_VIEW;
import static springsprout.modules.study.support.StudyURLRedirectionUtils.redirectStudyView;


@Controller
@RequestMapping("/study/")
@SessionAttributes("study")
public class StudyController {
	Logger log = LoggerFactory.getLogger(StudyController.class);

	private static final String STUDY_FORM = "study/form";
	private static final String STUDY_INDEX = "study/index";
	private static final String STUDY_VIEW = "study/view";
	private static final String REDIRECT_STUDY_INDEX = "redirect:/study/index";
	
	private static final String URL_STUDY_VIEW = "/study/view/";
	private static final String URL_STUDY_INDEX = "/study/index";
	
	@Resource StudyService advancedStudyService;
	@Autowired SecurityService securityService;
    @Autowired StudyStatisticsService statisticsService;

	@RequestMapping("index")
	public String index(@RequestParam(required = false) String type, Model model) {
		model.addAttribute("list", this.advancedStudyService.findActiveStudies());
        model.addAttribute("minitab_active", "active");
		return STUDY_INDEX;
    }
	
	@RequestMapping("index2")
	public String index2(@RequestParam(required = false) String type, Model model) {
		model.addAttribute("list", this.advancedStudyService.findActiveStudies());
        model.addAttribute("minitab_active", "active");
        model.addAttribute(advancedStudyService.getStudyById(5));
		return "study/index2";
    }
	
	@RequestMapping("index3")
	public String index3(@RequestParam(required = false) String type, Model model) {
		model.addAttribute( "list", this.advancedStudyService.findActiveStudies());
        model.addAttribute( "minitab_active", "active");
        model.addAttribute( advancedStudyService.findActiveStudies().get(0));
        model.addAttribute( "activeStudies", advancedStudyService.findActiveStudies());
        model.addAttribute( "studyIndexInfo", advancedStudyService.makeStudyIndexInfo());
		return "study/index3";
    }

	@RequestMapping("index/past")
	public String index(Model model) {
        model.addAttribute("list", this.advancedStudyService.findPastStudies());
		model.addAttribute("minitab_past", "active");
		return STUDY_INDEX;
	}
    
	@RequestMapping(value = "add", method = RequestMethod.GET)
	public String addForm(Model model) {
        model.addAttribute(new Study());
        model.addAttribute("title", "스터디 추가");
        model.addAttribute("backUrl", URL_STUDY_INDEX);
        model.addAttribute("isUpdate", false);
        return STUDY_FORM;
	}

	@RequestMapping(value = "add", method = RequestMethod.POST)
	public String addForm( @Valid Study study, BindingResult result, Model model, HttpSession session, SessionStatus status) {
		model.addAttribute("title", "스더티 추가");
		if (result.hasErrors()) return STUDY_FORM;
		advancedStudyService.addStudy(study);
        status.setComplete();
		setSession(session, study.getStudyName(), " 스터디가 개설되었습니다.");
		return REDIRECT_STUDY_INDEX;
	}

	@RequestMapping("view/{id}")
	public String view(@PathVariable int id, Model model) {
		Study study = advancedStudyService.getStudyById(id);
        model.addAttribute(study);
        model.addAttribute("memberCount", study.getMemberCount());
        model.addAttribute("isAlreadyJoinMember", advancedStudyService.isCurrentUserAlreadyJoinedIn(id));
        model.addAttribute("isManagerOrAdmin", advancedStudyService.isCurrentUserTheStudyManagerOrAdmin(id));
		return STUDY_VIEW;
	}

    @RequestMapping("/{id}")
	public String studyView(@PathVariable int id, Model model) {
        model.addAttribute(advancedStudyService.getStudyById(id));
        return "study/view2";
    }

    @RequestMapping("/{id}/summary")
	public String studySummary(@PathVariable int id, Model model) {
        model.addAttribute(advancedStudyService.getStudyById(id));
        return "study/view/summary";
    }

    @RequestMapping("/{id}/comments")
    public String studyComments(@PathVariable int id, Model model) {
        Study study = advancedStudyService.getStudyById(id);
    	model.addAttribute(study);
    	return "/study/view/comments";
    }

    @RequestMapping("/{id}/meetings")
    public String studyMeetings(@PathVariable int id, Model model) {
        Study study = advancedStudyService.getStudyById(id);
    	model.addAttribute(study);
        model.addAttribute("meetingWeekStatistics", statisticsService.getMeetingDayStatisticsOf(study.getMeetings()));
    	return "/study/view/meetings";
    }

    @RequestMapping("/{id}/members")
    public String studyMembers(@PathVariable int id, Model model) {
        Study study = advancedStudyService.getStudyById(id);
    	model.addAttribute(study);
    	return "/study/view/members";
    }

    @RequestMapping(value = "notify/{id}", method=RequestMethod.GET)
	public ModelAndView notify(@PathVariable int id, Model model, HttpSession session) {
    	advancedStudyService.notify(id);
        return new ModelAndView(JSON_VIEW).addObject("studyName", advancedStudyService.getStudyById(id).getStudyName());
	}

	@RequestMapping("delete/{id}")
	public String delete(@PathVariable int id, HttpSession session) {
		Study study = advancedStudyService.getStudyById(id);
		advancedStudyService.deleteStudy(study);
		setSession(session, study.getStudyName(), " 스터디가 폐쇄되었습니다.");
		return REDIRECT_STUDY_INDEX;
	}

	@RequestMapping(value = "update/{id}", method = RequestMethod.GET)
	public String updateForm(@PathVariable int id, Model model) {
		Study study = advancedStudyService.getStudyById(id);
		model.addAttribute(study);
		model.addAttribute("title", "스터디 수정");
        model.addAttribute("backUrl", URL_STUDY_VIEW + study.getId());
        model.addAttribute("isUpdate", true);
        return STUDY_FORM;
	}

	@RequestMapping(value = "update/{id}", method = RequestMethod.POST)
	public String updateForm(boolean isGoingToBeNotified, @Valid Study study, BindingResult result, HttpSession session, SessionStatus status)
			throws ServletRequestBindingException {
		if (result.hasErrors()) return STUDY_FORM;
		advancedStudyService.updateStudy(study, isGoingToBeNotified);
		status.setComplete();
        setSession(session, study.getStudyName(), " 스터디가 수정되었습니다.");
		return redirectStudyView(study.getId());
	}

	@RequestMapping("end/{id}")
	public String endStudy(HttpSession session, @PathVariable int id) {
		Study study = advancedStudyService.getStudyById(id);
		advancedStudyService.endStudy(study);
		setSession(session, study.getStudyName(), "스터디가 종료되었습니다.");
		return REDIRECT_STUDY_INDEX;
	}

	@RequestMapping("start/{id}")
	public String startStudy(HttpSession session, @PathVariable int id) {
		Study study = advancedStudyService.getStudyById(id);
		advancedStudyService.startStudy(study);
		setSession(session, study.getStudyName(), "스터디가 시작되었습니다.");
		return redirectStudyView(id);
	}

	@RequestMapping("join/{id}")
	public String addCurrentMember(HttpSession session, @PathVariable int id) {
		Study study = advancedStudyService.getStudyById(id);
		try {
            advancedStudyService.addCurrentMember(study);
            setSession(session, study.getStudyName(), " 스터디에 참석하셨습니다.");
        } catch (StudyMaximumOverException e){
        	setSession(session, study.getStudyName(), " 스터디 제한 인원을 확인하세요.");
            log.debug("Check study's maximum member count");
        }

		return redirectStudyView(id);
	}

	@RequestMapping("out/{id}")
	public String removeCurrentMember(HttpSession session, @PathVariable int id) {
		Study study = advancedStudyService.getStudyById(id);
		advancedStudyService.removeCurrentMember(study);
		setSession(session, study.getStudyName(), "스터디에 참석을 취소 하셨습니다.");
		return redirectStudyView(id);
	}
	
	@RequestMapping("view/{id}/meetings")
    public String viewMeeting( @PathVariable int id, Model model) {
		model.addAttribute(advancedStudyService.getStudyById(id));
		model.addAttribute("isAlreadyJoinMember", advancedStudyService.isCurrentUserAlreadyJoinedIn(id));
		model.addAttribute("isManagerOrAdmin", advancedStudyService.isCurrentUserTheStudyManagerOrAdmin(id));
    	return "/study/_meetings";
    }
	    
    @RequestMapping("view/{id}/meetingMembers")
    public String viewMeetingMembers( @PathVariable int id, Model model) {
    	model.addAttribute(advancedStudyService.getStudyById(id));
		model.addAttribute("isAlreadyJoinMember", advancedStudyService.isCurrentUserAlreadyJoinedIn(id));
		model.addAttribute("isManagerOrAdmin", advancedStudyService.isCurrentUserTheStudyManagerOrAdmin(id));
    	return "/study/_members";
    }
    
    @RequestMapping("view/{id}/updateTabDataCounts")
    @ResponseBody
    public CountInfoDTO updateTabDataCounts( @PathVariable int id) {
    	return new CountInfoDTO(advancedStudyService.getStudyById(id));
    }
    
    @RequestMapping("view/{id}/comments")
    public String viewComments( @PathVariable int id, Model model) {
        Study study = advancedStudyService.getStudyById(id);
    	model.addAttribute(study);
    	return "/study/_comments";
    }

	private void setSession(HttpSession session, String studyName, String msg) {
		session.setAttribute("SESSION_FLASH_MSG", studyName + msg);
	}

}
