/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.ResultCheckStyle;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.jdbc.Expectation;

/**
 * For persistence operations (INSERT, UPDATE, DELETE) what style of
 * determining results (success/failure) is to be used.
 *
 * @apiNote  This enumeration is mainly for internal use, since it
 *           is isomorphic to {@link ResultCheckStyle}. In the
 *           future, it would be nice to replace them both with a
 *           new {@code org.hibernate.ResultCheck} enum.
 *
 * @author Steve Ebersole
 *
 * @deprecated Use an {@link org.hibernate.jdbc.Expectation} class
 */
@Deprecated(since = "6.5", forRemoval = true)
public enum ExecuteUpdateResultCheckStyle {
	/**
	 * Do not perform checking.  Either user simply does not want checking, or is
	 * indicating a {@link java.sql.CallableStatement} execution in which the
	 * checks are being performed explicitly and failures are handled through
	 * propagation of {@link java.sql.SQLException}s.
	 */
	NONE,

	/**
	 * Perform row count checking.  Row counts are the int values returned by both
	 * {@link java.sql.PreparedStatement#executeUpdate()} and
	 * {@link java.sql.Statement#executeBatch()}.  These values are checked
	 * against some expected count.
	 */
	COUNT,

	/**
	 * Essentially the same as {@link #COUNT} except that the row count actually
	 * comes from an output parameter registered as part of a
	 * {@link java.sql.CallableStatement}.  This style explicitly prohibits
	 * statement batching from being used...
	 */
	PARAM;

	public String externalName() {
		switch (this) {
			case NONE:
				return "none";
			case COUNT:
				return "rowcount";
			case PARAM:
				return "param";
			default:
				throw new AssertionFailure("Unrecognized ExecuteUpdateResultCheckStyle");
		}
	}

	public static @Nullable ExecuteUpdateResultCheckStyle fromResultCheckStyle(ResultCheckStyle style) {
		switch (style) {
			case NONE:
				return NONE;
			case COUNT:
				return COUNT;
			case PARAM:
				return PARAM;
			default:
				return null;
		}
	}

	public static @Nullable ExecuteUpdateResultCheckStyle fromExternalName(String name) {
		for ( ExecuteUpdateResultCheckStyle style : values() ) {
			if ( style.externalName().equalsIgnoreCase(name) ) {
				return style;
			}
		}
		return null;
	}

	public static ExecuteUpdateResultCheckStyle determineDefault(@Nullable String customSql, boolean callable) {
		return customSql != null && callable ? PARAM : COUNT;
	}

	public static @Nullable Class<? extends Expectation> expectationClass(@Nullable ExecuteUpdateResultCheckStyle style) {
		if ( style == null ) {
			return null;
		}
		else {
			return style.expectationClass();
		}
	}

	public Class<? extends Expectation> expectationClass() {
		switch (this) {
			case NONE:
				return Expectation.None.class;
			case COUNT:
				return Expectation.RowCount.class;
			case PARAM:
				return Expectation.OutParameter.class;
			default:
				throw new AssertionFailure( "Unrecognized ExecuteUpdateResultCheckStyle");
		}
	}
}
