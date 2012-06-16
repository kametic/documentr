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
package de.blizzy.documentr.web.pagetree;

import static org.junit.Assert.*;

import org.junit.Test;

import de.blizzy.documentr.web.pagetree.AbstractTreeNode.Type;

public class AbstractTreeNodeTest {
	@Test
	public void getType() {
		assertSame(Type.PAGE, new AbstractTreeNode(Type.PAGE){}.getType());
	}
}