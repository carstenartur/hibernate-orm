/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Internal;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.spi.JdbcParameterRenderer;

/**
 * A SQL {@code UPDATE} statement.
 *
 * @author Gavin King
 */
@Internal
public class Update implements RestrictionRenderingContext {
	protected String tableName;
	protected String comment;
	protected Map<String,String> assignments = new LinkedHashMap<>();
	protected List<Restriction> restrictions = new ArrayList<>();

	private final JdbcParameterRenderer jdbcParameterRenderer;
	private final boolean standardParamRendering;
	private int parameterCount;

	public Update(SessionFactoryImplementor factory) {
		this( factory.getServiceRegistry().getService( JdbcParameterRenderer.class ) );
	}

	public Update(JdbcParameterRenderer jdbcParameterRenderer) {
		this.jdbcParameterRenderer = jdbcParameterRenderer;
		this.standardParamRendering = JdbcParameterRenderer.isStandardRenderer( jdbcParameterRenderer );
	}

	public String getTableName() {
		return tableName;
	}

	public Update setTableName(String tableName) {
		this.tableName = tableName;
		return this;
	}

	public Update setComment(String comment) {
		this.comment = comment;
		return this;
	}

	public Update addAssignments(String... columnNames) {
		for ( String columnName : columnNames ) {
			addAssignment( columnName );
		}
		return this;
	}

	public Update addAssignment(String columnName) {
		return addAssignment( columnName, "?" );
	}

	public Update addAssignment(String columnName, String valueExpression) {
		assignments.put( columnName, valueExpression );
		return this;
	}

	public Update addRestriction(String column) {
		restrictions.add( new ComparisonRestriction( column ) );
		return this;
	}

	public Update addRestriction(String... columns) {
		for ( int i = 0; i < columns.length; i++ ) {
			final String columnName = columns[ i ];
			if ( columnName != null ) {
				addRestriction( columnName );
			}
		}
		return this;
	}

	public Update addRestriction(String column, String value) {
		restrictions.add( new ComparisonRestriction( column, value ) );
		return this;
	}

	public Update addRestriction(String column, String op, String value) {
		restrictions.add( new ComparisonRestriction( column, op, value ) );
		return this;
	}

	private String normalizeExpressionFragment(String rhs) {
		return rhs.equals( "?" ) && !standardParamRendering
				? jdbcParameterRenderer.renderJdbcParameter( ++parameterCount, null )
				: rhs;
	}

	public Update addNullnessRestriction(String column) {
		restrictions.add( new NullnessRestriction( column ) );
		return this;
	}

	public String toStatementString() {
		final StringBuilder buf = new StringBuilder( ( assignments.size() * 15) + tableName.length() + 10 );

		applyComment( buf );
		buf.append( "update " ).append( tableName );
		applyAssignments( buf );
		applyRestrictions( buf );

		return buf.toString();
	}

	private void applyComment(StringBuilder buf) {
		if ( comment != null ) {
			buf.append( "/* " ).append( Dialect.escapeComment( comment ) ).append( " */ " );
		}
	}

	private void applyAssignments(StringBuilder buf) {
		buf.append( " set " );

		final Iterator<Map.Entry<String,String>> entries = assignments.entrySet().iterator();
		while ( entries.hasNext() ) {
			final Map.Entry<String,String> entry = entries.next();
			buf.append( entry.getKey() )
					.append( '=' )
					.append( normalizeExpressionFragment( entry.getValue() ) );
			if ( entries.hasNext() ) {
				buf.append( ", " );
			}
		}
	}

	private void applyRestrictions(StringBuilder buf) {
		if ( restrictions.isEmpty() ) {
			return;
		}

		buf.append( " where " );

		for ( int i = 0; i < restrictions.size(); i++ ) {
			if ( i > 0 ) {
				buf.append( " and " );
			}

			final Restriction restriction = restrictions.get( i );
			restriction.render( buf, this );
		}
	}

	@Override
	public String makeParameterMarker() {
		return jdbcParameterRenderer.renderJdbcParameter( ++parameterCount, null );
	}
}
