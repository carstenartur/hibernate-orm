/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.dialect.identity.DB2IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.sequence.DB2iSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.unique.AlterTableUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;

/**
 * An SQL dialect for i. This class provides support for DB2 Universal Database for i V7R1 and
 * later, also known as DB2/400.
 *
 * @author Pierrick Rouxel (pierrickrouxel)
 * @deprecated Use {@link DB2iDialect}
 */
@Deprecated
public class DB2400V7R3Dialect extends DB2400Dialect {

	private final UniqueDelegate uniqueDelegate;

	public DB2400V7R3Dialect() {
		super();

		uniqueDelegate = new AlterTableUniqueDelegate(this);
	}

	@Override
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return DB2iSequenceSupport.INSTANCE;
	}

	@Override
	public String getQuerySequencesString() {
		return "select distinct sequence_name from qsys2.syssequences " +
				"where current_schema='*LIBL' and sequence_schema in (select schema_name from qsys2.library_list_info) " +
				"or sequence_schema=current_schema";
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return DB2IdentityColumnSupport.INSTANCE;
	}
}
