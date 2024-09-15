/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.reflection;

import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKeyGroup;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

@JiraKeyGroup( value = {
		@JiraKey( value = "HHH-11924" ),
		@JiraKey( value = "HHH-14529" )
} )
public class ElementCollectionConverterTest extends BaseCoreFunctionalTestCase {

	@Override
	protected void prepareBootstrapRegistryBuilder(BootstrapServiceRegistryBuilder builder) {
		super.prepareBootstrapRegistryBuilder( builder );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
			Company.class,
		};
	}

	@Override
	protected String[] getOrmXmlFiles() {
		return new String[] { "org/hibernate/orm/test/annotations/reflection/element-collection-converter-orm.xml" };
	}


	@Test
	public void testConverterIsAppliedToElementCollection() {
		doInHibernate( this::sessionFactory, session -> {
			Company company = new Company();
			company.setId( 1L );

			Organization org1 = new Organization();
			org1.setOrganizationId( "ACME" );

			company.getOrganizations().add( org1 );

			session.persist( company );
		} );

		doInHibernate( this::sessionFactory, session -> {
			String organizationId = (String) session
					.createNativeQuery( "select organizations from Company_organizations" )
					.getSingleResult();
			assertEquals( "ORG-ACME", organizationId );

			Company company = session.find( Company.class, 1L );

			assertEquals( 1, company.getOrganizations().size() );
			assertEquals( "ACME" , company.getOrganizations().get( 0 ).getOrganizationId());
		} );
	}
}
