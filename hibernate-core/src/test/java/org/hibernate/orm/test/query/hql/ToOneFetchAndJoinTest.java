/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.orm.test.hql.fetchAndJoin.Entity1;
import org.hibernate.orm.test.hql.fetchAndJoin.Entity2;
import org.hibernate.orm.test.hql.fetchAndJoin.Entity3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
@DomainModel(
		annotatedClasses = {
				Entity1.class,
				Entity2.class,
				Entity3.class
		}
)
@SessionFactory
public class ToOneFetchAndJoinTest {

	@Test
	@JiraKey( value = "HHH-9637")
	public void testFetchJoinsWithImplicitJoinInRestriction(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final String qry =
							"select e1 " +
							"from Entity1 e1 " +
							"		inner join fetch e1.entity2 e2 " +
							"		inner join fetch e2.entity3 " +
							"where e1.entity2.value = 'entity2'";
					Entity1 e1Queryied = session.createQuery( qry, Entity1.class ).uniqueResult();
					assertEquals( "entity1", e1Queryied.getValue() );
					assertTrue( Hibernate.isInitialized( e1Queryied.getEntity2() ) );
					assertTrue( Hibernate.isInitialized( e1Queryied.getEntity2().getEntity3() ) );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-9637")
	public void testExplicitJoinBeforeFetchJoins(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final String qry =
							"select e1 " +
							"from Entity1 e1 " +
							"		inner join e1.entity2 e1Restrict " +
							"		inner join fetch e1.entity2 e2" +
							"		inner join fetch e2.entity3 " +
							"where e1Restrict.value = 'entity2'";
					Entity1 e1Queryied = session.createQuery( qry, Entity1.class ).uniqueResult();
					assertEquals( "entity1", e1Queryied.getValue() );
					assertTrue( Hibernate.isInitialized( e1Queryied.getEntity2() ) );
					assertTrue( Hibernate.isInitialized( e1Queryied.getEntity2().getEntity3() ) );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-9637")
	public void testExplicitJoinBetweenFetchJoins(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final String qry =
							"select e1 " +
							"from Entity1 e1 " +
							"		inner join fetch e1.entity2 e2 " +
							"		inner join e1.entity2 e1Restrict " +
							"		inner join fetch e2.entity3 " +
							"where e1Restrict.value = 'entity2'";
					Entity1 e1Queryied = session.createQuery( qry, Entity1.class ).uniqueResult();
					assertEquals( "entity1", e1Queryied.getValue() );
					assertTrue( Hibernate.isInitialized( e1Queryied.getEntity2() ) );
					assertTrue( Hibernate.isInitialized( e1Queryied.getEntity2().getEntity3() ) );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-9637")
	public void testExplicitJoinAfterFetchJoins(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final String qry =
							"select e1 " +
							"from Entity1 e1 " +
							"		inner join fetch e1.entity2 e2 " +
							"		inner join fetch e2.entity3 " +
							"		inner join e1.entity2 e1Restrict " +
							"where e1Restrict.value = 'entity2'";
					Entity1 e1Queryied = session.createQuery( qry, Entity1.class ).uniqueResult();
					assertEquals( "entity1", e1Queryied.getValue() );
					assertTrue( Hibernate.isInitialized( e1Queryied.getEntity2() ) );
					assertTrue( Hibernate.isInitialized( e1Queryied.getEntity2().getEntity3() ) );
				}
		);
	}

	@BeforeEach
	public void setupData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					Entity1 e1 = new Entity1();
					e1.setValue( "entity1" );
					Entity2 e2 = new Entity2();
					e2.setValue( "entity2" );
					Entity3 e3 = new Entity3();
					e3.setValue( "entity3" );

					e1.setEntity2( e2 );
					e2.setEntity3( e3 );

					Entity2 e2a = new Entity2();
					e2a.setValue( "entity2a" );

					session.persist( e3 );
					session.persist( e2 );
					session.persist( e1 );
					session.persist( e2a );
				}
		);
	}

	@AfterEach
	public void cleanupData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					session.createQuery( "delete Entity1" ).executeUpdate();
					session.createQuery( "delete Entity2" ).executeUpdate();
					session.createQuery( "delete Entity3" ).executeUpdate();
				}
		);
	}
}
