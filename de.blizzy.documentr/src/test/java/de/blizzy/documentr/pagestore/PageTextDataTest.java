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

import static de.blizzy.documentr.TestUtil.*;
import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;

import org.junit.Test;

public class PageTextDataTest {
	@Test
	public void getText() throws UnsupportedEncodingException {
		assertEquals("foo", new PageTextData("foo").getText()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("foo", PageTextData.fromBytes("foo".getBytes("UTF-8")).getText()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	@Test
	public void testEquals() {
		assertEqualsContract(
			new PageTextData("foo"), //$NON-NLS-1$
			new PageTextData("foo"), //$NON-NLS-1$
			new PageTextData("foo"), //$NON-NLS-1$
			new PageTextData("bar")); //$NON-NLS-1$
	}
}
