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
package de.blizzy.documentr.web.access;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.openid.OpenIDAuthenticationFilter;

import de.blizzy.documentr.Settings;

public class DocumentrOpenIdAuthenticationFilter extends OpenIDAuthenticationFilter {
	@Autowired
	private Settings settings;

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request,
			HttpServletResponse response) throws AuthenticationException,
			IOException {

		HttpServletRequest requestWrapper = new HttpServletRequestWrapper(request) {
			@Override
			public StringBuffer getRequestURL() {
				String url = super.getRequestURL().toString();
				String host = null;
				if (settings.getHost() != null) {
					host = settings.getHost();
					if (settings.getPort() != null) {
						host += ":" + String.valueOf(settings.getPort().intValue()); //$NON-NLS-1$
					}
				}
				if (host != null) {
					url = url.replaceFirst("^(http(?:s)?://)[^/]+/", "$1" + host + "/"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				return new StringBuffer(url);
			}
		};
		return super.attemptAuthentication(requestWrapper, response);
	}
}