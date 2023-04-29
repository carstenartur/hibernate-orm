/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.sql.DdlType;

import static org.hibernate.type.SqlTypes.ENUM;

/**
 * @author Gavin King
 */
public class NativeEnumDdlTypeImpl implements DdlType {
	private final Dialect dialect;

	public NativeEnumDdlTypeImpl(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public int getSqlTypeCode() {
		return ENUM;
	}

	@Override
	public String getTypeName(Size columnSize, Class<?> returnedClass) {
		return dialect.getEnumTypeDeclaration( (Class<? extends Enum<?>>) returnedClass );
	}

	@Override
	public String getRawTypeName() {
		// this
		return "enum";
	}

	@Override
	public String getTypeName(Long size, Integer precision, Integer scale) {
		return "varchar(" + size +  ")";
	}

	@Override
	public String getCastTypeName(JdbcType jdbcType, JavaType<?> javaType) {
		return "varchar";
	}

	@Override
	public String getCastTypeName(JdbcType jdbcType, JavaType<?> javaType, Long length, Integer precision, Integer scale) {
		return getTypeName( length, precision, scale );
	}
}
