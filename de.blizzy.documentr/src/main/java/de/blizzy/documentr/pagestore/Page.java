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
package de.blizzy.documentr.pagestore;

import java.io.UnsupportedEncodingException;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Page {
	private String parentPagePath;
	private String title;
	private byte[] data;
	private String contentType;

	private Page(String parentPagePath, String title, byte[] data, String contentType) {
		this.parentPagePath = parentPagePath;
		this.title = title;
		this.data = data;
		this.contentType = contentType;
	}

	public static Page fromText(String parentPagePath, String title, String text) {
		try {
			return new Page(parentPagePath, title, text.getBytes("UTF-8"), "text/plain"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static Page fromData(String parentPagePath, byte[] data, String contentType) {
		return new Page(parentPagePath, null, data, contentType);
	}

	public String getParentPagePath() {
		return parentPagePath;
	}
	
	public String getTitle() {
		return title;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public String getText() {
		try {
			return new String(data, "UTF-8"); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public String getContentType() {
		return contentType;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		} else if ((o != null) && o.getClass().equals(getClass())) {
			Page other = (Page) o;
			return new EqualsBuilder()
				.append(parentPagePath, other.parentPagePath)
				.append(title, other.title)
				.append(data, other.data)
				.append(contentType, other.contentType)
				.isEquals();
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder()
			.append(parentPagePath)
			.append(title)
			.append(data)
			.append(contentType)
			.toHashCode();
	}
}
