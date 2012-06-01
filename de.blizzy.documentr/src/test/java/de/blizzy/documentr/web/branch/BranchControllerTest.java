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
package de.blizzy.documentr.web.branch;

import static de.blizzy.documentr.DocumentrMatchers.*;
import static de.blizzy.documentr.TestUtil.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import de.blizzy.documentr.access.User;
import de.blizzy.documentr.access.UserStore;
import de.blizzy.documentr.pagestore.PageStore;
import de.blizzy.documentr.repository.GlobalRepositoryManager;

public class BranchControllerTest {
	private static final String PROJECT = "project"; //$NON-NLS-1$
	private static final String BRANCH = "branch"; //$NON-NLS-1$
	private static final User USER = new User("currentUser", "pw", "admin@example.com", false, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	
	private BranchController branchController;
	private GlobalRepositoryManager repoManager;
	private PageStore pageStore;
	private Authentication authentication;

	@Before
	public void setUp() throws IOException {
		repoManager = mock(GlobalRepositoryManager.class);
		pageStore = mock(PageStore.class);
		
		UserStore userStore = mock(UserStore.class);
		when(userStore.getUser(USER.getLoginName())).thenReturn(USER);

		branchController = new BranchController();
		branchController.setGlobalRepositoryManager(repoManager);
		branchController.setPageStore(pageStore);
		branchController.setUserStore(userStore);

		authentication = mock(Authentication.class);
		when(authentication.getName()).thenReturn(USER.getLoginName());
	}
	
	@Test
	public void createBranch() {
		Model model = mock(Model.class);
		String view = branchController.createBranch(PROJECT, model);
		assertEquals("/project/branch/edit", view); //$NON-NLS-1$
		
		verify(model).addAttribute(eq("branchForm"), //$NON-NLS-1$
				argBranchForm(PROJECT, null, null));
	}
	
	@Test
	public void saveFirstBranch() throws IOException, GitAPIException {
		when(repoManager.listProjectBranches(PROJECT)).thenReturn(Collections.<String>emptyList());
		
		BranchForm branchForm = new BranchForm(PROJECT, BRANCH, null);
		BindingResult bindingResult = new BeanPropertyBindingResult(branchForm, "branchForm"); //$NON-NLS-1$
		String view = branchController.saveBranch(branchForm, bindingResult, authentication);
		assertEquals("/page/edit/" + PROJECT + "/" + BRANCH + "/home", removeViewPrefix(view)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertRedirect(view);
		assertFalse(bindingResult.hasErrors());
		
		verify(repoManager).createProjectBranchRepository(PROJECT, BRANCH, null);
		verify(pageStore).savePage(eq(PROJECT), eq(BRANCH), eq("home"), //$NON-NLS-1$
				argPage(null, "Home", StringUtils.EMPTY), same(USER)); //$NON-NLS-1$
	}

	@Test
	public void saveBranch() throws IOException, GitAPIException {
		when(repoManager.listProjectBranches(PROJECT)).thenReturn(Arrays.asList("old_branch")); //$NON-NLS-1$
		
		BranchForm branchForm = new BranchForm(PROJECT, BRANCH, "old_branch"); //$NON-NLS-1$
		BindingResult bindingResult = new BeanPropertyBindingResult(branchForm, "branchForm"); //$NON-NLS-1$
		String view = branchController.saveBranch(branchForm, bindingResult, authentication);
		assertEquals("/page/" + PROJECT + "/" + BRANCH + "/home", removeViewPrefix(view)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertRedirect(view);
		assertFalse(bindingResult.hasErrors());
		
		verify(repoManager).createProjectBranchRepository(PROJECT, BRANCH, "old_branch"); //$NON-NLS-1$
	}
	
	@Test
	public void saveBranchButExists() throws IOException, GitAPIException {
		when(repoManager.listProjectBranches(PROJECT)).thenReturn(Arrays.asList(BRANCH));
		
		BranchForm branchForm = new BranchForm(PROJECT, BRANCH, "old_branch"); //$NON-NLS-1$
		BindingResult bindingResult = new BeanPropertyBindingResult(branchForm, "branchForm"); //$NON-NLS-1$
		String view = branchController.saveBranch(branchForm, bindingResult, authentication);
		assertEquals("/project/branch/edit", view); //$NON-NLS-1$
		assertTrue(bindingResult.hasErrors());
		assertTrue(bindingResult.hasFieldErrors("name")); //$NON-NLS-1$
	}
}
