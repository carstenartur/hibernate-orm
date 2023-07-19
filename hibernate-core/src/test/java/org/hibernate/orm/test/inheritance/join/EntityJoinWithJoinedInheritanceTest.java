/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.inheritance.join;

import java.util.List;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		EntityJoinWithJoinedInheritanceTest.BaseClass.class,
		EntityJoinWithJoinedInheritanceTest.ChildEntityA.class,
		EntityJoinWithJoinedInheritanceTest.SubChildEntityA1.class,
		EntityJoinWithJoinedInheritanceTest.SubChildEntityA2.class,
		EntityJoinWithJoinedInheritanceTest.ChildEntityB.class,
		EntityJoinWithJoinedInheritanceTest.RootOne.class
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-16438" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16494" )
public class EntityJoinWithJoinedInheritanceTest {
	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			s.createMutationQuery( "delete from RootOne" ).executeUpdate();
			s.createMutationQuery( "delete from SubChildEntityA1" ).executeUpdate();
			s.createMutationQuery( "delete from SubChildEntityA2" ).executeUpdate();
			s.createMutationQuery( "delete from BaseClass" ).executeUpdate();
		} );
	}

	@Test
	public void testSimpleLeftJoin(SessionFactoryScope scope) {
		scope.inTransaction( s -> s.persist( new RootOne( 1, null ) ) );
		scope.inTransaction( s -> {
			final List<RootOne> resultList = s.createQuery(
					"select r from RootOne r left join ChildEntityA ce on ce.id = r.childId",
					RootOne.class
			).getResultList();
			assertEquals( 1, resultList.size() );
		} );
	}

	@Test
	public void testLeftJoin(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			s.persist( new SubChildEntityA1( 11 ) );
			s.persist( new ChildEntityB( 21 ) );
			s.persist( new RootOne( 1, 11 ) );
			s.persist( new RootOne( 2, 21 ) );
		} );
		scope.inTransaction( s -> {
			final List<Tuple> resultList = s.createQuery(
					"select r, ce " +
							"from RootOne r left join ChildEntityA ce on ce.id = r.childId " +
							"order by r.id",
					Tuple.class
			).getResultList();
			assertEquals( 2, resultList.size() );
			assertResult( resultList.get( 0 ), 1, 11, 11, "child_a_1", SubChildEntityA1.class );
			assertResult( resultList.get( 1 ), 2, 21, null, null, null );
		} );
	}

	@Test
	public void testRightJoin(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			s.persist( new SubChildEntityA1( 11 ) );
			s.persist( new SubChildEntityA2( 12 ) );
			s.persist( new ChildEntityB( 21 ) );
			s.persist( new RootOne( 1, 11 ) );
			s.persist( new RootOne( 2, 11 ) );
			s.persist( new RootOne( 3, 21 ) );
		} );
		scope.inTransaction( s -> {
			final List<Tuple> resultList = s.createQuery(
					"select r, ce " +
							"from RootOne r right join ChildEntityA ce on ce.id = r.childId " +
							"order by r.id nulls last, ce.id",
					Tuple.class
			).getResultList();
			assertEquals( 3, resultList.size() );
			assertResult( resultList.get( 0 ), 1, 11, 11, "child_a_1", SubChildEntityA1.class );
			assertResult( resultList.get( 1 ), 2, 11, 11, "child_a_1", SubChildEntityA1.class );
			assertResult( resultList.get( 2 ), null, null, 12, "child_a_2", SubChildEntityA2.class );
		} );
	}

	@Test
	public void testCrossJoin(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			s.persist( new SubChildEntityA1( 11 ) );
			s.persist( new ChildEntityB( 21 ) );
			s.persist( new RootOne( 1, 11 ) );
			s.persist( new RootOne( 2, 21 ) );
		} );
		scope.inTransaction( s -> {
			final List<Tuple> resultList = s.createQuery(
					"select r, ce " +
							"from RootOne r cross join ChildEntityA ce " +
							"order by r.id nulls last, ce.id",
					Tuple.class
			).getResultList();
			assertEquals( 2, resultList.size() );
			assertResult( resultList.get( 0 ), 1, 11, 11, "child_a_1", SubChildEntityA1.class );
			assertResult( resultList.get( 1 ), 2, 21, 11, "child_a_1", SubChildEntityA1.class );
		} );
	}

	@Test
	@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsFullJoin.class )
	public void testFullJoin(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			s.persist( new SubChildEntityA1( 11 ) );
			s.persist( new SubChildEntityA2( 12 ) );
			s.persist( new ChildEntityB( 21 ) );
			s.persist( new RootOne( 1, 11 ) );
			s.persist( new RootOne( 2, 11 ) );
			s.persist( new RootOne( 3, 21 ) );
			s.persist( new RootOne( 4, null ) );
		} );
		scope.inTransaction( s -> {
			final List<Tuple> resultList = s.createQuery(
					"select r, ce " +
							"from RootOne r full join ChildEntityA ce on ce.id = r.childId " +
							"order by r.id nulls last, ce.id",
					Tuple.class
			).getResultList();
			assertEquals( 5, resultList.size() );
			assertResult( resultList.get( 0 ), 1, 11, 11, "child_a_1", SubChildEntityA1.class );
			assertResult( resultList.get( 1 ), 2, 11, 11, "child_a_1", SubChildEntityA1.class );
			assertResult( resultList.get( 2 ), 3, 21, null, null, null );
			assertResult( resultList.get( 3 ), 4, null, null, null, null );
			assertResult( resultList.get( 4 ), null, null, 12, "child_a_2", SubChildEntityA2.class );
		} );
	}

	private <T extends ChildEntityA> void assertResult(
			Tuple result,
			Integer rootId,
			Integer rootChildId,
			Integer childId,
			String discValue,
			Class<T> subClass) {
		if ( rootId != null ) {
			final RootOne root = result.get( 0, RootOne.class );
			assertEquals( rootId, root.getId() );
			assertEquals( rootChildId, root.getChildId() );
		}
		else {
			assertNull( result.get( 0 ) );
		}
		if ( subClass != null ) {
			assertInstanceOf( subClass, result.get( 1 ) );
			final ChildEntityA sub1 = result.get( 1, subClass );
			assertEquals( childId, sub1.getId() );
			assertEquals( discValue, sub1.getDiscCol() );
		}
		else {
			assertNull( result.get( 1 ) );
		}
	}

	/**
	 * NOTE: We define a {@link DiscriminatorColumn} to allow multiple subclasses
	 * to share the same table name. This will need additional care when pruning
	 * the table expression, since we'll have to add the discriminator condition
	 * before joining with the subclass tables
	 */
	@Entity( name = "BaseClass" )
	@Inheritance( strategy = InheritanceType.JOINED )
	@DiscriminatorColumn( name = "disc_col" )
	public static class BaseClass {
		@Id
		private Integer id;

		@Column( name = "disc_col", insertable = false, updatable = false )
		private String discCol;

		public BaseClass() {
		}

		public BaseClass(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public String getDiscCol() {
			return discCol;
		}
	}

	@Entity( name = "ChildEntityA" )
	@Table( name = "child_entity" )
	public static abstract class ChildEntityA extends BaseClass {
		public ChildEntityA() {
		}

		public ChildEntityA(Integer id) {
			super( id );
		}
	}

	@Entity( name = "SubChildEntityA1" )
	@DiscriminatorValue( "child_a_1" )
	public static class SubChildEntityA1 extends ChildEntityA {
		public SubChildEntityA1() {
		}

		public SubChildEntityA1(Integer id) {
			super( id );
		}
	}

	@Entity( name = "SubChildEntityA2" )
	@DiscriminatorValue( "child_a_2" )
	public static class SubChildEntityA2 extends ChildEntityA {
		public SubChildEntityA2() {
		}

		public SubChildEntityA2(Integer id) {
			super( id );
		}
	}

	@Entity( name = "ChildEntityB" )
	@Table( name = "child_entity" )
	public static class ChildEntityB extends BaseClass {

		public ChildEntityB() {
		}

		public ChildEntityB(Integer id) {
			super( id );
		}
	}

	@Entity( name = "RootOne" )
	public static class RootOne {
		@Id
		private Integer id;
		private Integer childId;

		public RootOne() {
		}

		public RootOne(Integer id, Integer childId) {
			this.id = id;
			this.childId = childId;
		}

		public Integer getId() {
			return id;
		}

		public Integer getChildId() {
			return childId;
		}
	}
}
