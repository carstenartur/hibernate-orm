/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) {DATE}, Red Hat Inc. or third-party contributors as
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
package org.hibernate.orm.test.jpa.criteria.selectcase;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

@JiraKey( value = "HHH-9731" )
public class SelectCaseTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Entity.class};
	}

	@Test
	public void selectCaseWithValuesShouldWork() {
		EntityManager entityManager = getOrCreateEntityManager();
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();

		CriteriaBuilder.Case<EnumValue> selectCase = cb.selectCase();
		Predicate somePredicate = cb.equal( cb.literal( 1 ), 1 );
		selectCase.when( somePredicate, EnumValue.VALUE_1 );
		selectCase.otherwise( EnumValue.VALUE_2 );

		CriteriaQuery<Entity> query = cb.createQuery( Entity.class );
		Root<Entity> from = query.from( Entity.class );
		query.select( from ).where( cb.equal( from.get( "value" ), selectCase ) );

		entityManager.createQuery( query ).getResultList();
	}

	@Test
	public void selectCaseWithCastedTypeValuesShouldWork() {
		EntityManager entityManager = getOrCreateEntityManager();

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();

		CriteriaBuilder.Case<String> selectCase = cb.selectCase();
		Predicate somePredicate = cb.equal( cb.literal( 1 ), 1 );
		selectCase.when( somePredicate, EnumValue.VALUE_1.name() );
		selectCase.otherwise( EnumValue.VALUE_2.name() );

		CriteriaQuery<Entity> query = cb.createQuery( Entity.class );
		Root<Entity> from = query.from( Entity.class );
		query.select( from ).where( cb.equal( from.get( "value" ).as( String.class ), selectCase.as( String.class ) ) );

		entityManager.createQuery( query ).getResultList();
	}

	@Test
	public void simpleSelectCaseWithValuesShouldWork() {
		EntityManager entityManager = getOrCreateEntityManager();

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();

		CriteriaBuilder.SimpleCase<Integer, EnumValue> selectCase = cb.selectCase( cb.literal( 1 ) );
		selectCase.when( 1, EnumValue.VALUE_1 );
		selectCase.otherwise( EnumValue.VALUE_2 );

		CriteriaQuery<Entity> query = cb.createQuery( Entity.class );
		Root<Entity> from = query.from( Entity.class );
		query.select( from ).where( cb.equal( from.get( "value" ), selectCase ) );

		List<?> result = entityManager.createQuery( query ).getResultList();
	}

	@Test
	public void simpleSelectCaseWithCastedTypeValuesShouldWork() {
		EntityManager entityManager = getOrCreateEntityManager();

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();

		CriteriaBuilder.SimpleCase<Integer, String> selectCase = cb.selectCase( cb.literal( 1 ) );
		selectCase.when( 1, EnumValue.VALUE_1.name() );
		selectCase.otherwise( EnumValue.VALUE_2.name() );

		CriteriaQuery<Entity> query = cb.createQuery( Entity.class );
		Root<Entity> from = query.from( Entity.class );
		query.select( from ).where( cb.equal( from.get( "value" ).as( String.class ), selectCase.as( String.class ) ) );

		entityManager.createQuery( query ).getResultList();
	}

	@jakarta.persistence.Entity
	@Table(name = "entity")
	public static class Entity {

		@Id
		private Long id;

		@Enumerated(EnumType.STRING)
		@Column(name = "val")
		private EnumValue value;
	}

	public enum EnumValue {
		VALUE_1,
		VALUE_2;
	}
}
