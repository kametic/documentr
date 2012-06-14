/*
documentr - Edit, maintain, and present software documentation on the web.
Copyright (C) 2012 Maik Schreiber

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.blizzy.documentr.web.page;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import de.blizzy.documentr.DocumentrConstants;
import de.blizzy.documentr.Util;
import de.blizzy.documentr.access.AuthenticationUtil;
import de.blizzy.documentr.access.User;
import de.blizzy.documentr.access.UserStore;
import de.blizzy.documentr.page.IPageStore;
import de.blizzy.documentr.page.Page;
import de.blizzy.documentr.page.PageMetadata;
import de.blizzy.documentr.page.PageNotFoundException;
import de.blizzy.documentr.page.PageTextData;
import de.blizzy.documentr.page.PageUtil;
import de.blizzy.documentr.repository.GlobalRepositoryManager;
import de.blizzy.documentr.web.ErrorController;
import de.blizzy.documentr.web.markdown.MarkdownProcessor;

@Controller
@RequestMapping("/page")
public class PageController {
	@Autowired
	private IPageStore pageStore;
	@Autowired
	private GlobalRepositoryManager repoManager;
	@Autowired
	private MarkdownProcessor markdownProcessor;
	@Autowired
	private UserStore userStore;
	
	@RequestMapping(value="/{projectName:" + DocumentrConstants.PROJECT_NAME_PATTERN + "}/" +
			"{branchName:" + DocumentrConstants.BRANCH_NAME_PATTERN + "}/" +
			"{path:" + DocumentrConstants.PAGE_PATH_URL_PATTERN + "}",
			method=RequestMethod.GET)
	@PreAuthorize("hasPagePermission(#projectName, #branchName, #path, 'VIEW')")
	public String getPage(@PathVariable String projectName, @PathVariable String branchName,
			@PathVariable String path, Model model, HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		try {
			path = Util.toRealPagePath(path);
			PageMetadata metadata = pageStore.getPageMetadata(projectName, branchName, path);

			long lastEdited = metadata.getLastEdited().getTime();
			long authenticationCreated = AuthenticationUtil.getAuthenticationCreationTime(request.getSession());
			long projectEditTime = PageUtil.getProjectEditTime(projectName);
			long lastModified = Math.max(lastEdited, authenticationCreated);
			if (projectEditTime >= 0) {
				lastModified = Math.max(lastModified, projectEditTime);
			}

			long modifiedSince = request.getDateHeader("If-Modified-Since"); //$NON-NLS-1$
			if ((modifiedSince >= 0) && (lastModified <= modifiedSince)) {
				return ErrorController.notModified();
			}

			response.setDateHeader("Last-Modified", lastModified); //$NON-NLS-1$
			response.setDateHeader("Expires", 0); //$NON-NLS-1$
			response.setHeader("Cache-Control", "must-revalidate, private"); //$NON-NLS-1$ //$NON-NLS-2$

			Page page = pageStore.getPage(projectName, branchName, path, false);
			model.addAttribute("path", path); //$NON-NLS-1$
			model.addAttribute("pageName", //$NON-NLS-1$
					path.contains("/") ? StringUtils.substringAfterLast(path, "/") : path); //$NON-NLS-1$ //$NON-NLS-2$
			model.addAttribute("parentPagePath", page.getParentPagePath()); //$NON-NLS-1$
			model.addAttribute("title", page.getTitle()); //$NON-NLS-1$
			return "/project/branch/page/view"; //$NON-NLS-1$
		} catch (PageNotFoundException e) {
			return ErrorController.notFound("page.notFound"); //$NON-NLS-1$
		}
	}

	@RequestMapping(value="/create/{projectName:" + DocumentrConstants.PROJECT_NAME_PATTERN + "}/" +
			"{branchName:" + DocumentrConstants.BRANCH_NAME_PATTERN + "}/" +
			"{parentPagePath:" + DocumentrConstants.PAGE_PATH_URL_PATTERN + "}", method=RequestMethod.GET)
	@PreAuthorize("hasBranchPermission(#projectName, #branchName, 'EDIT_PAGE')")
	public String createPage(@PathVariable String projectName, @PathVariable String branchName,
			@PathVariable String parentPagePath, Model model) {

		PageForm form = new PageForm(projectName, branchName, null,
				Util.toRealPagePath(parentPagePath), null, null);
		model.addAttribute("pageForm", form); //$NON-NLS-1$
		return "/project/branch/page/edit"; //$NON-NLS-1$
	}
	
	@RequestMapping(value="/edit/{projectName:" + DocumentrConstants.PROJECT_NAME_PATTERN + "}/" +
			"{branchName:" + DocumentrConstants.BRANCH_NAME_PATTERN + "}/" +
			"{path:" + DocumentrConstants.PAGE_PATH_URL_PATTERN + "}",
			method=RequestMethod.GET)
	@PreAuthorize("hasPagePermission(#projectName, #branchName, #path, 'EDIT_PAGE')")
	public String editPage(@PathVariable String projectName, @PathVariable String branchName,
			@PathVariable String path, Model model) throws IOException {
		
		try {
			path = Util.toRealPagePath(path);
			Page page = pageStore.getPage(projectName, branchName, path, true);
			PageForm form = new PageForm(projectName, branchName,
					path, page.getParentPagePath(),
					page.getTitle(), ((PageTextData) page.getData()).getText());
			model.addAttribute("pageForm", form); //$NON-NLS-1$
			return "/project/branch/page/edit"; //$NON-NLS-1$
		} catch (PageNotFoundException e) {
			return ErrorController.notFound("page.notFound"); //$NON-NLS-1$
		}
	}
	
	@RequestMapping(value="/save/{projectName:" + DocumentrConstants.PROJECT_NAME_PATTERN + "}/" +
			"{branchName:" + DocumentrConstants.BRANCH_NAME_PATTERN + "}", method=RequestMethod.POST)
	@PreAuthorize("hasPagePermission(#projectName, #branchName, #path, 'EDIT_PAGE')")
	public String savePage(@ModelAttribute @Valid PageForm form, BindingResult bindingResult,
			Authentication authentication) throws IOException {
		
		if (!repoManager.listProjectBranches(form.getProjectName()).contains(form.getBranchName())) {
			bindingResult.rejectValue("branchName", "page.branch.nonexistent"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (bindingResult.hasErrors()) {
			return "/project/branch/page/edit"; //$NON-NLS-1$
		}

		String parentPagePath = form.getParentPagePath();
		if (StringUtils.isBlank(parentPagePath)) {
			parentPagePath = null;
		}
		parentPagePath = Util.toRealPagePath(parentPagePath);
		Page page = Page.fromText(parentPagePath, form.getTitle(), form.getText());
		String path = form.getPath();
		if (StringUtils.isBlank(path)) {
			path = parentPagePath + "/" + Util.simplifyForURL(form.getTitle()); //$NON-NLS-1$
		}
		
		Page oldPage = null;
		try {
			oldPage = pageStore.getPage(form.getProjectName(), form.getBranchName(), path, true);
		} catch (PageNotFoundException e) {
			// okay
		}
		if ((oldPage == null) || !page.equals(oldPage)) {
			User user = userStore.getUser(authentication.getName());
			pageStore.savePage(form.getProjectName(), form.getBranchName(), path, page, user);
		}

		return "redirect:/page/" + form.getProjectName() + "/" + form.getBranchName() + "/" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			Util.toURLPagePath(path);
	}
	
	@ModelAttribute
	public PageForm createPageForm(@PathVariable String projectName, @PathVariable String branchName,
			@RequestParam(required=false) String path, @RequestParam(required=false) String parentPagePath,
			@RequestParam(required=false) String title, @RequestParam(required=false) String text) {
		
		return ((path != null) && (title != null) && (text != null)) ?
				new PageForm(projectName, branchName, path, parentPagePath, title, text) : null;
	}
	
	@RequestMapping(value="/generateName/{projectName:" + DocumentrConstants.PROJECT_NAME_PATTERN + "}/" +
			"{branchName:" + DocumentrConstants.BRANCH_NAME_PATTERN + "}/" +
			"{parentPagePath:" + DocumentrConstants.PAGE_PATH_URL_PATTERN + "}/json",
			method=RequestMethod.POST, produces="application/json")
	@ResponseBody
	@PreAuthorize("hasBranchPermission(#projectName, #branchName, 'VIEW')")
	public Map<String, Object> generateName(@PathVariable String projectName, @PathVariable String branchName,
			@PathVariable String parentPagePath, @RequestParam String title) throws IOException {

		String name = Util.simplifyForURL(title);
		String path = Util.toRealPagePath(parentPagePath) + "/" + name; //$NON-NLS-1$
		boolean pageExists = false;
		try {
			Page page = pageStore.getPage(projectName, branchName, path, false);
			pageExists = page != null;
		} catch (PageNotFoundException e) {
			// okay
		}

		Map<String, Object> result = new HashMap<String, Object>();
		result.put("path", path); //$NON-NLS-1$
		result.put("exists", Boolean.valueOf(pageExists)); //$NON-NLS-1$
		return result;
	}

	@RequestMapping(value="/markdownToHTML/{projectName:" + DocumentrConstants.PROJECT_NAME_PATTERN + "}/" +
			"{branchName:" + DocumentrConstants.BRANCH_NAME_PATTERN + "}/json",
			method=RequestMethod.POST, produces="application/json")
	@ResponseBody
	@PreAuthorize("isAuthenticated()")
	public Map<String, String> markdownToHTML(@PathVariable String projectName, @PathVariable String branchName,
			@RequestParam String markdown, @RequestParam(required=false) String pagePath, Authentication authentication) {

		Map<String, String> result = new HashMap<String, String>();
		String html = markdownProcessor.markdownToHTML(markdown, projectName, branchName, pagePath, authentication);
		html = markdownProcessor.processNonCacheableMacros(html, projectName, branchName, pagePath, authentication);
		result.put("html", html); //$NON-NLS-1$
		return result;
	}
	
	@RequestMapping(value="/copyToBranch/{projectName:" + DocumentrConstants.PROJECT_NAME_PATTERN + "}/" +
			"{branchName:" + DocumentrConstants.BRANCH_NAME_PATTERN + "}/" +
			"{path:" + DocumentrConstants.PAGE_PATH_URL_PATTERN + "}",
			method=RequestMethod.POST)
	@PreAuthorize("hasPagePermission(#projectName, #branchName, #path, 'VIEW') and " +
			"hasBranchPermission(#projectName, #targetBranchName, 'EDIT_PAGE')")
	public String copyToBranch(@PathVariable String projectName, @PathVariable String branchName,
			@PathVariable String path, @RequestParam String targetBranchName, Authentication authentication)
			throws IOException {

		path = Util.toRealPagePath(path);
		Page page = pageStore.getPage(projectName, branchName, path, true);
		User user = userStore.getUser(authentication.getName());
		pageStore.savePage(projectName, targetBranchName, path, page, user);
		return "redirect:/page/edit/" + projectName + "/" + targetBranchName + "/" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				Util.toURLPagePath(path);
	}

	@RequestMapping(value="/delete/{projectName:" + DocumentrConstants.PROJECT_NAME_PATTERN + "}/" +
			"{branchName:" + DocumentrConstants.BRANCH_NAME_PATTERN + "}/" +
			"{path:" + DocumentrConstants.PAGE_PATH_URL_PATTERN + "}",
			method=RequestMethod.GET)
	@PreAuthorize("hasBranchPermission(#projectName, #branchName, 'EDIT_PAGE')")
	public String deletePage(@PathVariable String projectName, @PathVariable String branchName,
			@PathVariable String path, Authentication authentication) throws IOException {
		
		path = Util.toRealPagePath(path);
		User user = userStore.getUser(authentication.getName());
		pageStore.deletePage(projectName, branchName, path, user);
		return "redirect:/page/" + projectName + "/" + branchName + //$NON-NLS-1$ //$NON-NLS-2$
				"/" + DocumentrConstants.HOME_PAGE_NAME; //$NON-NLS-1$
	}
	
	@RequestMapping(value="/relocate/{projectName:" + DocumentrConstants.PROJECT_NAME_PATTERN + "}/" +
			"{branchName:" + DocumentrConstants.BRANCH_NAME_PATTERN + "}/" +
			"{path:" + DocumentrConstants.PAGE_PATH_URL_PATTERN + "}",
			method=RequestMethod.POST)
	@PreAuthorize("hasBranchPermission(#projectName, #branchName, 'EDIT_PAGE')")
	public String relocatePage(@PathVariable String projectName, @PathVariable String branchName,
			@PathVariable String path, @RequestParam String newParentPagePath, Authentication authentication)
			throws IOException {
		
		path = Util.toRealPagePath(path);
		newParentPagePath = Util.toRealPagePath(newParentPagePath);
		
		User user = userStore.getUser(authentication.getName());
		pageStore.relocatePage(projectName, branchName, path, newParentPagePath, user);
		String pageName = path.contains("/") ? StringUtils.substringAfterLast(path, "/") : path; //$NON-NLS-1$ //$NON-NLS-2$
		return "redirect:/page/" + projectName + "/" + branchName + //$NON-NLS-1$ //$NON-NLS-2$
				"/" + Util.toURLPagePath(newParentPagePath + "/" + pageName); //$NON-NLS-1$ //$NON-NLS-2$
	}

	void setPageStore(IPageStore pageStore) {
		this.pageStore = pageStore;
	}

	void setGlobalRepositoryManager(GlobalRepositoryManager repoManager) {
		this.repoManager = repoManager;
	}

	void setMarkdownProcessor(MarkdownProcessor markdownProcessor) {
		this.markdownProcessor = markdownProcessor;
	}

	void setUserStore(UserStore userStore) {
		this.userStore = userStore;
	}
}
