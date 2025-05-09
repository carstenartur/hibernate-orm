/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter.subclass.tableperclass;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.orm.test.filter.subclass.SubClassTest;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;

@DomainModel(
		annotatedClasses = {
				Animal.class, Mammal.class, Human.class
		}
)
@SessionFactory
public class TablePerClassTest extends SubClassTest {
	@Override
	protected void persistTestData(SessionImplementor session) {
		createHuman( session, false, 90 );
		createHuman( session, false, 100 );
		createHuman( session, true, 110 );
	}

	private void createHuman(SessionImplementor session, boolean pregnant, int iq) {
		Human human = new Human();
		human.setName( "Homo Sapiens" );
		human.setPregnant( pregnant );
		human.setIq( iq );
		session.persist( human );
	}
}
