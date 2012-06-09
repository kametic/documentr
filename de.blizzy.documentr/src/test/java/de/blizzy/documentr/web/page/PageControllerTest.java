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

import static de.blizzy.documentr.DocumentrMatchers.*;
import static de.blizzy.documentr.TestUtil.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import de.blizzy.documentr.DocumentrConstants;
import de.blizzy.documentr.Util;
import de.blizzy.documentr.access.User;
import de.blizzy.documentr.access.UserStore;
import de.blizzy.documentr.page.IPageStore;
import de.blizzy.documentr.page.Page;
import de.blizzy.documentr.page.PageMetadata;
import de.blizzy.documentr.page.PageNotFoundException;
import de.blizzy.documentr.repository.GlobalRepositoryManager;
import de.blizzy.documentr.web.markdown.MarkdownProcessor;

public class PageControllerTest {
	private static final String PROJECT = "project"; //$NON-NLS-1$
	private static final String BRANCH = "branch"; //$NON-NLS-1$
	private static final String PAGE_PATH = DocumentrConstants.HOME_PAGE_NAME + "/page"; //$NON-NLS-1$
	private static final String PAGE_PATH_URL = DocumentrConstants.HOME_PAGE_NAME + ",page"; //$NON-NLS-1$
	private static final String PAGE_NAME = "page"; //$NON-NLS-1$
	private static final String PARENT_PAGE = DocumentrConstants.HOME_PAGE_NAME;
	private static final User USER = new User("currentUser", "pw", "admin@example.com", false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	
	private IPageStore pageStore;
	private GlobalRepositoryManager repoManager;
	private PageController pageController;
	private Authentication authenticatedAuthentication;
	private Authentication anonymousAuthentication;

	@Before
	@SuppressWarnings("boxing")
	public void setUp() throws IOException {
		pageStore = mock(IPageStore.class);
		repoManager = mock(GlobalRepositoryManager.class);
		
		UserStore userStore = mock(UserStore.class);
		when(userStore.getUser(USER.getLoginName())).thenReturn(USER);

		pageController = new PageController();
		pageController.setPageStore(pageStore);
		pageController.setGlobalRepositoryManager(repoManager);
		pageController.setMarkdownProcessor(new MarkdownProcessor());
		pageController.setUserStore(userStore);

		authenticatedAuthentication = mock(Authentication.class);
		when(authenticatedAuthentication.isAuthenticated()).thenReturn(true);
		when(authenticatedAuthentication.getName()).thenReturn(USER.getLoginName());

		anonymousAuthentication = mock(Authentication.class);
		when(authenticatedAuthentication.isAuthenticated()).thenReturn(false);
	}
	
	@Test
	@SuppressWarnings("boxing")
	public void getPage() throws IOException {
		HttpSession session = mock(HttpSession.class);
		when(session.getAttribute("authenticationCreationTime")).thenReturn(System.currentTimeMillis()); //$NON-NLS-1$

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getDateHeader(anyString())).thenReturn(-1L);
		when(request.getSession()).thenReturn(session);
		
		getPage(request);
	}

	@Test
	@SuppressWarnings("boxing")
	public void getPageMustReturnNormallyIfModified() throws IOException {
		HttpSession session = mock(HttpSession.class);
		when(session.getAttribute("authenticationCreationTime")).thenReturn(System.currentTimeMillis()); //$NON-NLS-1$

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getDateHeader("If-Modified-Since")).thenReturn( //$NON-NLS-1$
				new GregorianCalendar(2000, Calendar.JANUARY, 1).getTimeInMillis());
		when(request.getSession()).thenReturn(session);
		
		getPage(request);
	}
	
	private void getPage(HttpServletRequest request) throws IOException {
		HttpServletResponse response = mock(HttpServletResponse.class);

		Date lastModified = new Date();
		when(pageStore.getPageMetadata(PROJECT, BRANCH, PAGE_PATH)).thenReturn(new PageMetadata("user", lastModified)); //$NON-NLS-1$
		
		Page page = Page.fromText(PARENT_PAGE, "title", "text"); //$NON-NLS-1$ //$NON-NLS-2$
		when(pageStore.getPage(PROJECT, BRANCH, PAGE_PATH, false)).thenReturn(page);
		
		Model model = mock(Model.class);
		SecurityContextHolder.setContext(createSecurityContext(anonymousAuthentication));
		String view = pageController.getPage(PROJECT, BRANCH, PAGE_PATH_URL, model, request, response);
		SecurityContextHolder.clearContext();
		assertEquals("/project/branch/page/view", view); //$NON-NLS-1$
		
		verify(model).addAttribute("path", PAGE_PATH); //$NON-NLS-1$
		verify(model).addAttribute("pageName", PAGE_NAME); //$NON-NLS-1$
		verify(model).addAttribute("parentPagePath", PARENT_PAGE); //$NON-NLS-1$
		verify(model).addAttribute("title", page.getTitle()); //$NON-NLS-1$
		verify(response).setDateHeader("Last-Modified", lastModified.getTime()); //$NON-NLS-1$
	}
	
	@Test
	@SuppressWarnings("boxing")
	public void getPageMustReturn404IfNotFound() throws IOException {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getDateHeader(anyString())).thenReturn(-1L);
		
		HttpServletResponse response = mock(HttpServletResponse.class);
		
		when(pageStore.getPageMetadata(eq(PROJECT), eq(BRANCH), eq("nonexistent"))) //$NON-NLS-1$
			.thenThrow(new PageNotFoundException(PROJECT, BRANCH, "nonexistent")); //$NON-NLS-1$
		
		Model model = mock(Model.class);
		SecurityContextHolder.setContext(createSecurityContext(authenticatedAuthentication));
		String view = pageController.getPage(PROJECT, BRANCH, "nonexistent", model, request, response); //$NON-NLS-1$
		SecurityContextHolder.clearContext();
		assertEquals("/error/" + HttpServletResponse.SC_NOT_FOUND + "/page.notFound", removeViewPrefix(view)); //$NON-NLS-1$ //$NON-NLS-2$
		assertForward(view);
	}
	
	@Test
	@SuppressWarnings("boxing")
	public void getPageMustReturn304IfNotModified() throws IOException {
		HttpSession session = mock(HttpSession.class);
		when(session.getAttribute("authenticationCreationTime")).thenReturn(
				new GregorianCalendar(2012, Calendar.JUNE, 2).getTime().getTime()); //$NON-NLS-1$

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getDateHeader("If-Modified-Since")).thenReturn( //$NON-NLS-1$
				new GregorianCalendar(2012, Calendar.JUNE, 9).getTimeInMillis());
		when(request.getSession()).thenReturn(session);
		
		HttpServletResponse response = mock(HttpServletResponse.class);
		
		when(pageStore.getPageMetadata(eq(PROJECT), eq(BRANCH), eq("nonexistent"))) //$NON-NLS-1$
			.thenReturn(new PageMetadata("user", new GregorianCalendar(2012, Calendar.JUNE, 1).getTime())); //$NON-NLS-1$
		
		Model model = mock(Model.class);
		SecurityContextHolder.setContext(createSecurityContext(anonymousAuthentication));
		String view = pageController.getPage(PROJECT, BRANCH, "nonexistent", model, request, response); //$NON-NLS-1$
		SecurityContextHolder.clearContext();
		assertTrue(removeViewPrefix(view).startsWith("/error/" + HttpServletResponse.SC_NOT_MODIFIED + "/")); //$NON-NLS-1$ //$NON-NLS-2$
		assertForward(view);
	}
	
	@Test
	public void createPage() {
		Model model = mock(Model.class);
		String view = pageController.createPage(PROJECT, BRANCH, PARENT_PAGE, model);
		assertEquals("/project/branch/page/edit", view); //$NON-NLS-1$
		
		verify(model).addAttribute(eq("pageForm"), //$NON-NLS-1$
				argPageForm(PROJECT, BRANCH, null, PARENT_PAGE, null, null));
	}
	
	@Test
	public void editPage() throws IOException {
		Page page = Page.fromText(PARENT_PAGE, "title", "text"); //$NON-NLS-1$ //$NON-NLS-2$
		when(pageStore.getPage(PROJECT, BRANCH, PAGE_PATH, true)).thenReturn(page);
		
		Model model = mock(Model.class);
		String view = pageController.editPage(PROJECT, BRANCH, PAGE_PATH_URL, model);
		assertEquals("/project/branch/page/edit", view); //$NON-NLS-1$
		
		verify(model).addAttribute(eq("pageForm"), //$NON-NLS-1$
				argPageForm(PROJECT, BRANCH, PAGE_PATH, PARENT_PAGE, "title", "text")); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	@Test
	public void editPageButNonexistent() throws IOException {
		when(pageStore.getPage(eq(PROJECT), eq(BRANCH), eq("nonexistent"), anyBoolean())) //$NON-NLS-1$
			.thenThrow(new PageNotFoundException(PROJECT, BRANCH, "nonexistent")); //$NON-NLS-1$
		
		Model model = mock(Model.class);
		String view = pageController.editPage(PROJECT, BRANCH, "nonexistent", model); //$NON-NLS-1$
		assertEquals("/error/" + HttpServletResponse.SC_NOT_FOUND + "/page.notFound", removeViewPrefix(view)); //$NON-NLS-1$ //$NON-NLS-2$
		assertForward(view);
	}
	
	@Test
	public void savePage() throws IOException {
		when(repoManager.listProjectBranches(PROJECT)).thenReturn(Collections.singletonList(BRANCH));
		PageForm pageForm = new PageForm(PROJECT, BRANCH, PAGE_PATH, PARENT_PAGE, "title", "text"); //$NON-NLS-1$ //$NON-NLS-2$
		BindingResult bindingResult = new BeanPropertyBindingResult(pageForm, "pageForm"); //$NON-NLS-1$
		
		String view = pageController.savePage(pageForm, bindingResult, authenticatedAuthentication);
		assertEquals("/page/" + PROJECT + "/" + BRANCH + "/" + PAGE_PATH_URL, removeViewPrefix(view)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertRedirect(view);
		assertFalse(bindingResult.hasErrors());
		
		verify(pageStore).savePage(eq(PROJECT), eq(BRANCH), eq(PAGE_PATH),
				argPage(PARENT_PAGE, "title", "text"), same(USER)); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	@Test
	public void savePageMustNotChangeExistingPath() throws IOException {
		when(repoManager.listProjectBranches(PROJECT)).thenReturn(Collections.singletonList(BRANCH));
		PageForm pageForm = new PageForm(PROJECT, BRANCH, PAGE_PATH, PARENT_PAGE, "title", "text"); //$NON-NLS-1$ //$NON-NLS-2$
		BindingResult bindingResult = new BeanPropertyBindingResult(pageForm, "pageForm"); //$NON-NLS-1$
		pageController.savePage(pageForm, bindingResult, authenticatedAuthentication);
		
		Page page = Page.fromText(PARENT_PAGE, "title", "text"); //$NON-NLS-1$ //$NON-NLS-2$
		when(pageStore.getPage(PROJECT, BRANCH, PAGE_PATH, true)).thenReturn(page);
		pageForm = new PageForm(PROJECT, BRANCH, PAGE_PATH, PARENT_PAGE, "title2", "text2"); //$NON-NLS-1$ //$NON-NLS-2$
		bindingResult = new BeanPropertyBindingResult(pageForm, "pageForm"); //$NON-NLS-1$

		String view = pageController.savePage(pageForm, bindingResult, authenticatedAuthentication);
		assertEquals("/page/" + PROJECT + "/" + BRANCH + "/" + PAGE_PATH_URL, removeViewPrefix(view)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertRedirect(view);
		assertFalse(bindingResult.hasErrors());

		verify(pageStore).savePage(eq(PROJECT), eq(BRANCH), eq(PAGE_PATH),
				argPage(PARENT_PAGE, "title2", "text2"), same(USER)); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	@Test
	public void savePageBlankPath() throws IOException {
		when(repoManager.listProjectBranches(PROJECT)).thenReturn(Collections.singletonList(BRANCH));
		String title = "title"; //$NON-NLS-1$
		PageForm pageForm = new PageForm(PROJECT, BRANCH, StringUtils.EMPTY, PARENT_PAGE, title, "text"); //$NON-NLS-1$
		BindingResult bindingResult = new BeanPropertyBindingResult(pageForm, "pageForm"); //$NON-NLS-1$
		
		String view = pageController.savePage(pageForm, bindingResult, authenticatedAuthentication);
		String path = PARENT_PAGE + "/" + Util.simplifyForURL(title); //$NON-NLS-1$
		String pathUrl = Util.toURLPagePath(path);
		assertEquals("/page/" + PROJECT + "/" + BRANCH + "/" + pathUrl, removeViewPrefix(view)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertRedirect(view);
		assertFalse(bindingResult.hasErrors());
		
		verify(pageStore).savePage(eq(PROJECT), eq(BRANCH), eq(path),
				argPage(PARENT_PAGE, title, "text"), same(USER)); //$NON-NLS-1$
	}
	
	@Test
	public void savePageShouldDoNothingIfNoChanges() throws IOException {
		when(repoManager.listProjectBranches(PROJECT)).thenReturn(Collections.singletonList(BRANCH));
		Page page = Page.fromText(PARENT_PAGE, "title", "text"); //$NON-NLS-1$ //$NON-NLS-2$
		when(pageStore.getPage(PROJECT, BRANCH, PAGE_PATH, true)).thenReturn(page);
		
		PageForm pageForm = new PageForm(PROJECT, BRANCH, PAGE_PATH, PARENT_PAGE, "title", "text"); //$NON-NLS-1$ //$NON-NLS-2$
		BindingResult bindingResult = new BeanPropertyBindingResult(pageForm, "pageForm"); //$NON-NLS-1$
		pageController.savePage(pageForm, bindingResult, authenticatedAuthentication);

		verify(pageStore, never()).savePage(
				anyString(), anyString(), anyString(), Matchers.<Page>any(), Matchers.<User>any());
	}
	
	@Test
	public void savePageButNonexistentBranch() throws IOException {
		when(repoManager.listProjectBranches(PROJECT)).thenReturn(Collections.singletonList(BRANCH));
		PageForm pageForm = new PageForm(PROJECT, "nonexistent", PAGE_PATH, PARENT_PAGE, "title", "text"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		BindingResult bindingResult = new BeanPropertyBindingResult(pageForm, "pageForm"); //$NON-NLS-1$
		
		String view = pageController.savePage(pageForm, bindingResult, authenticatedAuthentication);
		assertEquals("/project/branch/page/edit", view); //$NON-NLS-1$
		assertTrue(bindingResult.hasErrors());
		assertTrue(bindingResult.hasFieldErrors("branchName")); //$NON-NLS-1$
	}
	
	@Test
	public void generateName() throws IOException {
		String title = "simple as 1, 2, 3"; //$NON-NLS-1$
		String path = PARENT_PAGE + "/" + Util.simplifyForURL(title); //$NON-NLS-1$
		when(pageStore.getPage(eq(PROJECT), eq(BRANCH), eq(path), anyBoolean()))
			.thenThrow(new PageNotFoundException(PROJECT, BRANCH, path));
		Map<String, Object> result = pageController.generateName(PROJECT, BRANCH, PARENT_PAGE, title);
		assertEquals(path, result.get("path")); //$NON-NLS-1$
		assertEquals(Boolean.FALSE, result.get("exists")); //$NON-NLS-1$

		title = "title"; //$NON-NLS-1$
		path = PARENT_PAGE + "/" + Util.simplifyForURL(title); //$NON-NLS-1$
		Page page = Page.fromText(PARENT_PAGE, title, "text"); //$NON-NLS-1$
		when(pageStore.getPage(PROJECT, BRANCH, path, false)).thenReturn(page);
		result = pageController.generateName(PROJECT, BRANCH, PARENT_PAGE, title);
		assertEquals(path, result.get("path")); //$NON-NLS-1$
		assertEquals(Boolean.TRUE, result.get("exists")); //$NON-NLS-1$
	}
	
	@Test
	public void markdownToHTML() {
		Map<String, String> result = pageController.markdownToHTML(PROJECT, BRANCH, "**foo**", PAGE_PATH, //$NON-NLS-1$
				authenticatedAuthentication);
		assertEquals("<p><strong>foo</strong></p>", result.get("html")); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	@Test
	public void copyToBranch() throws IOException {
		Page page = Page.fromText(PARENT_PAGE, "title", "text"); //$NON-NLS-1$ //$NON-NLS-2$
		when(pageStore.getPage(PROJECT, BRANCH, PAGE_PATH, true)).thenReturn(page);
		
		String view = pageController.copyToBranch(PROJECT, BRANCH, PAGE_PATH_URL, "targetBranch", //$NON-NLS-1$
				authenticatedAuthentication);
		assertEquals("/page/edit/" + PROJECT + "/targetBranch/" + PAGE_PATH_URL, removeViewPrefix(view)); //$NON-NLS-1$ //$NON-NLS-2$
		assertRedirect(view);
		
		verify(pageStore).savePage(eq(PROJECT), eq("targetBranch"), eq(PAGE_PATH), //$NON-NLS-1$
				argPage(PARENT_PAGE, "title", "text"), same(USER)); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	@Test
	public void deletePage() throws IOException {
		String view = pageController.deletePage(PROJECT, BRANCH, PAGE_PATH_URL, authenticatedAuthentication);
		assertEquals("/page/" + PROJECT + "/" + BRANCH + "/" + DocumentrConstants.HOME_PAGE_NAME, removeViewPrefix(view)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertRedirect(view);
		
		verify(pageStore).deletePage(PROJECT, BRANCH, PAGE_PATH, USER);
	}
}
