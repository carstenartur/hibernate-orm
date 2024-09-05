/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.generator;

import org.hibernate.Incubating;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.insert.GetGeneratedKeysDelegate;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.id.insert.InsertReturningDelegate;
import org.hibernate.id.insert.UniqueKeySelectingDelegate;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.generator.internal.NaturalIdHelper.getNaturalIdPropertyNames;
import static org.hibernate.generator.values.internal.GeneratedValuesHelper.noCustomSql;

/**
 * A generator which produces a new value by actually going ahead and writing a row to the
 * database, then retrieving the value which was generated by the database itself as a side
 * effect of the SQL {@code insert} or {@code update} statement which wrote the row.
 * <p>
 * A value generated by the database might be generated implicitly, by a trigger, or using
 * a {@code default} column value specified in DDL, for example, or it might be generated
 * by a SQL expression occurring explicitly in the SQL {@code insert} or {@code update}
 * statement.
 * <p>
 * The generated value is usually retrieved from the database using a SQL {@code select},
 * but in {@linkplain #getGeneratedIdentifierDelegate certain cases} this additional round
 * trip may be avoided.
 * <p>
 * Implementations should override {@link #referenceColumnsInSql(Dialect)},
 * {@link #writePropertyValue()}, and {@link #getReferencedColumnValues(Dialect)} as needed
 * in order to achieve the desired behavior.
 * <p>
 * In implementation of this interface does not specify how the generated value is retrieved
 * from the database after it is generated, this being the responsibility of the coordinating
 * code in {@link org.hibernate.metamodel.mapping.internal.GeneratedValuesProcessor} or in an
 * implementation of {@link org.hibernate.id.insert.InsertGeneratedIdentifierDelegate}.
 *
 * @author Steve Ebersole
 * @author Gavin King
 *
 * @since 6.2
 */
public interface OnExecutionGenerator extends Generator {

	/**
	 * Determines if the columns whose values are generated are included in the column list
	 * of the SQL {@code insert} or {@code update} statement. For example, this method should
	 * return:
	 * <ul>
	 * <li>{@code true} if the value is generated by calling a SQL function like
	 *     {@code current_timestamp}, or
	 * <li>{@code false} if the value is generated by a trigger,
	 *     by {@link org.hibernate.annotations.GeneratedColumn generated always as}, or
	 *     using a {@linkplain org.hibernate.annotations.ColumnDefault column default value}.
	 * </ul>
	 *
	 * @return {@code true} if the column is included in the column list of the SQL statement.
	 */
	boolean referenceColumnsInSql(Dialect dialect);

	/**
	 * Determines if the property values are written to JDBC as the argument of a JDBC {@code ?}
	 * parameter.
	 */
	boolean writePropertyValue();

	/**
	 * A SQL expression indicating how to calculate the generated values when the mapped columns
	 * are {@linkplain #referenceColumnsInSql(Dialect) included in the SQL statement}. The SQL
	 * expressions might be:
	 * <ul>
	 * <li>function calls like {@code current_timestamp} or {@code nextval('mysequence')}, or
	 * <li>syntactic markers like {@code default}.
	 * </ul>
	 *
	 * @param dialect The {@linkplain Dialect SQL dialect}, allowing generation of an expression
	 *				  in dialect-specific SQL.
	 * @return The column value to be used in the generated SQL statement.
	 */
	String[] getReferencedColumnValues(Dialect dialect);

	/**
	 * The {@link InsertGeneratedIdentifierDelegate} used to retrieve the generated value if this
	 * object is an identifier generator.
	 * <p>
	 * This is ignored by {@link org.hibernate.metamodel.mapping.internal.GeneratedValuesProcessor},
	 * which handles multiple generators at once. So if this object is not an identifier generator,
	 * this method is never called.
	 * <p>
	 * Note that this method arguably breaks the separation of concerns between the generator and
	 * coordinating code, by specifying how the generated value should be <em>retrieved</em>.
	 * <p>
	 * The problem solved here is: we want to obtain an insert-generated primary key. But, sadly,
	 * without already knowing the primary key, there's no completely-generic way to locate the
	 * just-inserted row to obtain it.
	 * <p>
	 * We need one of the following things:
	 * <ul>
	 *     <li>a database which supports some form of {@link Dialect#supportsInsertReturning()
	 *         insert ... returning} syntax, or can do the same thing using the JDBC
	 *         {@link Dialect#supportsInsertReturningGeneratedKeys() getGeneratedKeys()} API, or
	 *     <li>a second unique key of the entity, that is, a property annotated
	 *         {@link org.hibernate.annotations.NaturalId @NaturalId}.
	 * </ul>
	 * <p>
	 * Alternatively, if the generated id is an identity/"autoincrement" column, we can take
	 * advantage of special platform-specific functionality to retrieve it. Taking advantage
	 * of the specialness of identity columns is the job of one particular implementation:
	 * {@link org.hibernate.id.IdentityGenerator}. And the need for customized behavior for
	 * identity columns is the reason why this layer-breaking method exists.
	 */
	@Incubating
	default InsertGeneratedIdentifierDelegate getGeneratedIdentifierDelegate(EntityPersister persister) {
		final SessionFactoryImplementor factory = persister.getFactory();
		final Dialect dialect = factory.getJdbcServices().getDialect();
		if ( dialect.supportsInsertReturningGeneratedKeys()
				&& factory.getSessionFactoryOptions().isGetGeneratedKeysEnabled() ) {
			return new GetGeneratedKeysDelegate( persister, false, INSERT );
		}
		else if ( dialect.supportsInsertReturning() && noCustomSql( persister, INSERT ) ) {
			return new InsertReturningDelegate( persister, INSERT );
		}
		else {
			// let's just hope the entity has a @NaturalId!
			return new UniqueKeySelectingDelegate( persister, getUniqueKeyPropertyNames( persister ), INSERT );
		}
	}

	/**
	 * The name of a property of the entity which may be used to locate the just-{@code insert}ed
	 * row containing the generated value. Of course, the columns mapped by this property should
	 * form a unique key of the entity.
	 * <p>
	 * The default implementation uses the {@link org.hibernate.annotations.NaturalId @NaturalId}
	 * property, if there is one.
	 */
	@Incubating
	default String[] getUniqueKeyPropertyNames(EntityPersister persister) {
		return getNaturalIdPropertyNames( persister );
	}

	@Override
	default boolean generatedOnExecution() {
		return true;
	}
}
