package springsprout.modules.study.board.imagePost;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;

import springsprout.common.web.support.Paging;
import springsprout.domain.Comment;
import springsprout.domain.Study;
import springsprout.domain.study.board.ImagePost;
import springsprout.modules.study.board.PostService;
import springsprout.service.security.SecurityService;

@Controller
@RequestMapping("/study/view/{id}/board/imagePost")
@SessionAttributes({"study"})
public class ImagePostController {

	@Autowired @Qualifier("imagePostService") PostService<ImagePost> service;
	@Autowired SecurityService securityService;
	
	@RequestMapping("/list/{page}")
	public String getList(Model model, @ModelAttribute Study study, @PathVariable int page, SessionStatus status) {
		List<ImagePost> posts = service.getList( page, Paging.DEFAULT_SIZE);
		model.addAttribute(new ImagePost(study));
		model.addAttribute("posts", posts);
		model.addAttribute("page", page);
		model.addAttribute("comment", new Comment());
		return "study/board/imagePost/list";
	}
	
	@RequestMapping(method = RequestMethod.POST)
	public String write(Model model, @ModelAttribute ImagePost post, HttpServletRequest request) {
		service.addPost(post);
		return "study/board/imagePost/list/0";
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public String getForm(Model model, @ModelAttribute Study study) {
		model.addAttribute("imagePost", new ImagePost(study));
		model.addAttribute("title", "이미지 추가");
		model.addAttribute("action", "/study/view/" + study.getId() + "/board/imagePost");
		return "study/board/imagePost/form";
	}
	
	@RequestMapping(value="/{postId}/update", method = RequestMethod.POST)
	public String update(@PathVariable int postId, ModelMap model, @ModelAttribute ImagePost post) {
		post.setId(postId);
		service.updatePost(post);
		return "study/board/imagePost/list";	
	}
	
	@RequestMapping(value="/{postId}", method = RequestMethod.GET)
	public String getUpdateForm(@PathVariable int postId, Model model, @ModelAttribute Study study, HttpServletRequest request) {
		model.addAttribute("imagePost", service.getPost(postId));
		model.addAttribute("title", "이미지 수정");
		model.addAttribute("action", "/study/view/" + study.getId() + "/board/imagePost/" + postId + "/update");
		return "study/board/imagePost/form";
	}
	
	@RequestMapping(value="/{postId}", method = RequestMethod.DELETE)
	public String remove(@PathVariable int postId, ImagePost post, HttpServletRequest request) {
		post.setId(postId);
		service.removePost(post);
		return "study/board/imagePost/list";
	}
	
	@RequestMapping(value="/{id}/comment/write", method = RequestMethod.POST)
	public String writeComment(@PathVariable int id, Model model, Comment comment) {
		ImagePost post = service.getPost(id);
		service.addComment(post, comment);
		model.addAttribute("postId", id);
		return "study/board/_commentSpot";
	}
	
	@RequestMapping(value="/{postId}/comment/{commentId}", method = RequestMethod.DELETE)
	@ResponseBody
	public boolean removeComment( @PathVariable int postId, @PathVariable int commentId) {
		service.removeComment( postId, commentId);
		return true;
	}
}
