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
package de.blizzy.documentr.web.filter;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class TrimResponseWrapperTest {
	@Test
	public void setContentLengthMustBeIgnored() {
		HttpServletResponse response = mock(HttpServletResponse.class);
		TrimResponseWrapper wrapper = new TrimResponseWrapper(response);
		wrapper.setContentLength(123);
		verify(response, never()).setContentLength(anyInt());
	}
	
	@Test
	public void getOutputStreamAndGetData() throws IOException {
		HttpServletResponse response = mock(HttpServletResponse.class);
		TrimResponseWrapper wrapper = new TrimResponseWrapper(response);
		byte[] data = "hello \u20AC".getBytes("UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$
		
		ServletOutputStream out = null;
		try {
			out = wrapper.getOutputStream();
			out.write(data);
		} finally {
			IOUtils.closeQuietly(out);
		}
		
		assertTrue(Arrays.equals(data, wrapper.getData()));
	}

	@Test
	public void getWriterAndGetData() throws IOException {
		HttpServletResponse response = mock(HttpServletResponse.class);
		when(response.getCharacterEncoding()).thenReturn("UTF-8"); //$NON-NLS-1$
		
		TrimResponseWrapper wrapper = new TrimResponseWrapper(response);
		String s = "hello \u20AC"; //$NON-NLS-1$

		PrintWriter out = null;
		try {
			out = wrapper.getWriter();
			out.write(s);
		} finally {
			IOUtils.closeQuietly(out);
		}
		
		assertTrue(Arrays.equals(s.getBytes("UTF-8"), wrapper.getData())); //$NON-NLS-1$
	}
}
