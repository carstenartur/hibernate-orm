/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.jdbc.spi;

import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.entity.EntityFetch;
import org.hibernate.sql.results.spi.RowReader;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * State pertaining to the processing of a single "row" of a JdbcValuesSource
 *
 * @author Steve Ebersole
 */
public interface RowProcessingState extends ExecutionContext {
	/**
	 * Access to the state related to the overall processing of the results.
	 */
	JdbcValuesSourceProcessingState getJdbcValuesSourceProcessingState();

	/**
	 * Retrieve the value corresponding to the given SqlSelection as part
	 * of the "current JDBC row".
	 *
	 * @see SqlSelection#getValuesArrayPosition()
	 * @see #getJdbcValue(int)
	 */
	default Object getJdbcValue(SqlSelection sqlSelection) {
		return getJdbcValue( sqlSelection.getValuesArrayPosition() );
	}

	/**
	 * todo (6.0) : do we want this here?  Depends how we handle caching assembler / result memento
	 */
	RowReader<?> getRowReader();

	/**
	 * Retrieve the value corresponding to the given index as part
	 * of the "current JDBC row".
	 *
	 * We read all the ResultSet values for the given row one time
	 * and store them into an array internally based on the principle that multiple
	 * accesses to this array will be significantly faster than accessing them
	 * from the ResultSet potentially multiple times.
	 */
	Object getJdbcValue(int position);

	void registerNonExists(EntityFetch fetch);

	boolean isQueryCacheHit();

	/**
	 * Callback at the end of processing the current "row"
	 *
	 * @deprecated Use {@link #finishRowProcessing(boolean)} instead
	 */
	@Deprecated(forRemoval = true)
	void finishRowProcessing();

	/**
	 * Callback at the end of processing the current "row"
	 */
	default void finishRowProcessing(boolean wasAdded) {
		finishRowProcessing();
	}

	/**
	 * Locate the Initializer registered for the given path
	 */
	Initializer resolveInitializer(@Nullable NavigablePath path);
}
