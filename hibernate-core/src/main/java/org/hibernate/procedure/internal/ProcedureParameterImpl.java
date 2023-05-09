/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.procedure.internal;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.BindableType;
import org.hibernate.query.OutputableType;
import org.hibernate.query.internal.BindingTypeHelper;
import org.hibernate.query.spi.AbstractQueryParameter;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.sql.exec.internal.JdbcCallParameterExtractorImpl;
import org.hibernate.sql.exec.internal.JdbcCallParameterRegistrationImpl;
import org.hibernate.sql.exec.internal.JdbcCallRefCursorExtractorImpl;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.type.BasicType;
import org.hibernate.type.ProcedureParameterNamedBinder;

import jakarta.persistence.ParameterMode;

/**
 * @author Steve Ebersole
 */
public class ProcedureParameterImpl<T> extends AbstractQueryParameter<T> implements ProcedureParameterImplementor<T> {

	private final String name;
	private final Integer position;
	private final ParameterMode mode;
	private final Class<T> javaType;

	public ProcedureParameterImpl(
			String name,
			ParameterMode mode,
			Class<T> javaType,
			BindableType<T> hibernateType) {
		super( false, hibernateType );
		this.name = name;
		this.position = null;
		this.mode = mode;
		this.javaType = javaType;
	}

	public ProcedureParameterImpl(
			Integer position,
			ParameterMode mode,
			Class<T> javaType,
			BindableType<T> hibernateType) {
		super( false, hibernateType );
		this.name = null;
		this.position = position;
		this.mode = mode;
		this.javaType = javaType;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Integer getPosition() {
		return position;
	}

	@Override
	public ParameterMode getMode() {
		return mode;
	}

	@Override
	public Class<T> getParameterType() {
		return javaType;
	}

	@Override
	public NamedCallableQueryMemento.ParameterMemento toMemento() {
		return session -> {
			if ( getName() != null ) {
				return new ProcedureParameterImpl<>(
						getName(),
						getMode(),
						javaType,
						getHibernateType()
				);
			}
			else {
				return new ProcedureParameterImpl<>(
						getPosition(),
						getMode(),
						javaType,
						getHibernateType()
				);
			}
		};
	}

	@Override
	public JdbcCallParameterRegistration toJdbcParameterRegistration(
			int startIndex,
			ProcedureCallImplementor<?> procedureCall) {
		final QueryParameterBinding<T> binding = procedureCall.getParameterBindings().getBinding( this );
		final OutputableType<T> typeToUse = (OutputableType<T>) BindingTypeHelper.INSTANCE.resolveTemporalPrecision(
				binding == null || binding.getExplicitTemporalPrecision() == null
						? null
						: binding.getExplicitTemporalPrecision(),
				getHibernateType(),
				procedureCall.getSession().getFactory()
		);

		final String name;
		if ( procedureCall.getParameterStrategy() == ParameterStrategy.NAMED
				&& canDoNameParameterBinding( typeToUse, procedureCall ) ) {
			name = this.name;
		}
		else {
			name = null;
		}

		final JdbcParameterBinder parameterBinder;
		final JdbcCallRefCursorExtractorImpl refCursorExtractor;
		final JdbcCallParameterExtractorImpl<T> parameterExtractor;

		switch ( mode ) {
			case REF_CURSOR:
				refCursorExtractor = new JdbcCallRefCursorExtractorImpl( name, startIndex );
				parameterBinder = null;
				parameterExtractor = null;
				break;
			case IN:
				parameterBinder = getParameterBinder( typeToUse, name );
				parameterExtractor = null;
				refCursorExtractor = null;
				break;
			case INOUT:
				parameterBinder = getParameterBinder( typeToUse, name );
				parameterExtractor = new JdbcCallParameterExtractorImpl<>( procedureCall.getProcedureName(), name, startIndex, typeToUse );
				refCursorExtractor = null;
				break;
			default:
				parameterBinder = null;
				parameterExtractor = new JdbcCallParameterExtractorImpl<>( procedureCall.getProcedureName(), name, startIndex, typeToUse );
				refCursorExtractor = null;
				break;
		}

		return new JdbcCallParameterRegistrationImpl( name, startIndex, mode, typeToUse, parameterBinder, parameterExtractor, refCursorExtractor );
	}

	private JdbcParameterBinder getParameterBinder(BindableType<T> typeToUse, String name) {
		if ( typeToUse instanceof BasicType<?> ) {
			if ( name == null ) {
				return new JdbcParameterImpl( (BasicType<T>) typeToUse );
			}
			else {
				return new JdbcParameterImpl( (BasicType<T>) typeToUse ) {
					@Override
					protected void bindParameterValue(
							JdbcMapping jdbcMapping,
							PreparedStatement statement,
							Object bindValue,
							int startPosition,
							ExecutionContext executionContext) throws SQLException {
						jdbcMapping.getJdbcValueBinder().bind(
								(CallableStatement) statement,
								bindValue,
								name,
								executionContext.getSession()
						);
					}
					@Override
					public String toString() {
						return "JdbcParameter(" + name + ")";
					}
				};
			}
		}
		else if ( typeToUse == null ) {
			throw new IllegalArgumentException( "Cannot determine the bindable type for procedure parameter: " + name );
		}
		else {
			throw new UnsupportedOperationException();
		}
	}

	private boolean canDoNameParameterBinding(
			BindableType<?> hibernateType,
			ProcedureCallImplementor<?> procedureCall) {
		final ExtractedDatabaseMetaData databaseMetaData = procedureCall.getSession()
				.getFactory()
				.getJdbcServices()
				.getJdbcEnvironment()
				.getExtractedDatabaseMetaData();
		return procedureCall.getFunctionReturn() == null
				&& databaseMetaData.supportsNamedParameters()
				&& hibernateType instanceof ProcedureParameterNamedBinder
				&& ( (ProcedureParameterNamedBinder<?>) hibernateType ).canDoSetting();
	}

	@Override
	public int hashCode() {
		return Objects.hash( name, position, mode );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		ProcedureParameterImpl<?> that = (ProcedureParameterImpl<?>) o;
		return Objects.equals( name, that.name ) &&
				Objects.equals( position, that.position ) &&
				mode == that.mode;
	}

	@Override
	public String toString() {
		if ( position == null ) {
			return name;
		}
		else {
			return position.toString();
		}
	}
}
