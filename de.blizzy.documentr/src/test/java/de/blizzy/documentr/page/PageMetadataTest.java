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
package de.blizzy.documentr.page;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;

import de.blizzy.documentr.page.PageMetadata;

public class PageMetadataTest {
	@Test
	public void getLastEditedBy() {
		Date lastEdited = new Date();
		PageMetadata metadata = new PageMetadata("user", lastEdited); //$NON-NLS-1$
		assertEquals("user", metadata.getLastEditedBy()); //$NON-NLS-1$
	}

	@Test
	public void getLastEdited() {
		Date lastEdited = new Date();
		PageMetadata metadata = new PageMetadata("user", lastEdited); //$NON-NLS-1$
		assertEquals(lastEdited, metadata.getLastEdited());
	}
}