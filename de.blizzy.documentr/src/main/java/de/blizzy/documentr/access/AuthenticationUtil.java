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

import javax.servlet.http.HttpSession;

public final class AuthenticationUtil {
	private static final String AUTHENTICATION_CREATION_TIME = "authenticationCreationTime"; //$NON-NLS-1$

	private AuthenticationUtil() {}
	
	public static void setAuthenticationCreationTime(HttpSession session, long time) {
		session.setAttribute(AUTHENTICATION_CREATION_TIME, Long.valueOf(time));
	}
	
	public static long getAuthenticationCreationTime(HttpSession session) {
		Long time = (Long) session.getAttribute(AUTHENTICATION_CREATION_TIME);
		return (time != null) ? time.longValue() / 1000L * 1000L : 0;
	}
}
