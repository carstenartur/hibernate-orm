/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.noentity;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestUtil;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Gavin King
 */
public class NoEntityTest extends CompilationTest {
	@Test
	@WithClasses({ Dao.class })
	public void testGeneratedAnnotationNotGenerated() {
		System.out.println( TestUtil.getMetaModelSourceAsString( Dao.class ) );
		assertMetamodelClassGeneratedFor( Dao.class );
	}
}
