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
package de.blizzy.documentr.access;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class DocumentrAnonymousAuthentication extends AnonymousAuthenticationToken {
	private static final List<? extends GrantedAuthority> AUTHORITIES =
			Collections.unmodifiableList(Arrays.asList(
					new PermissionGrantedAuthority(GrantedAuthorityTarget.APPLICATION, Permission.VIEW)
			));
	
	public DocumentrAnonymousAuthentication(String key, Object principal) {
		super(key, principal, new ArrayList<GrantedAuthority>(AUTHORITIES));
	}
}
