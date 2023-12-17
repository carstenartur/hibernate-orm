/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

/**
 * A {@link LimitHandler} for databases like PostgreSQL, H2,
 * and HSQL that support the syntax {@code LIMIT n OFFSET m}.
 * Note that this syntax does not allow specification of an
 * offset without a limit.
 */
public class LimitOffsetLimitHandler extends AbstractSimpleLimitHandler {

	public static LimitOffsetLimitHandler INSTANCE = new LimitOffsetLimitHandler();
	public static LimitOffsetLimitHandler OFFSET_ONLY_INSTANCE = new LimitOffsetLimitHandler() {
		@Override
		protected String offsetOnlyClause() {
			return " offset ?";
		}
	};

	@Override
	protected String limitClause(boolean hasFirstRow) {
		return hasFirstRow ? " limit ? offset ?" : " limit ?";
	}

	@Override
	protected String offsetOnlyClause() {
		return " limit " + Integer.MAX_VALUE + " offset ?";
	}

	@Override
	public final boolean bindLimitParametersInReverseOrder() {
		return true;
	}

	@Override
	public boolean supportsOffset() {
		return true;
	}
}
