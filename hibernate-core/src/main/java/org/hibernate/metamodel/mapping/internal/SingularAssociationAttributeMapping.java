/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAliasStemHelper;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupBuilder;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.from.TableReferenceCollector;
import org.hibernate.sql.results.internal.domain.entity.DelayedEntityFetch;
import org.hibernate.sql.results.internal.domain.entity.EntityFetchImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;

/**
 * @author Steve Ebersole
 */
public class SingularAssociationAttributeMapping extends AbstractSingularAttributeMapping
		implements EntityValuedModelPart, TableGroupJoinProducer {
	private final String sqlAliasStem;
	private final ForeignKeyDescriptor foreignKeyDescriptor;

	public SingularAssociationAttributeMapping(
			String name,
			int stateArrayPosition,
			ForeignKeyDescriptor foreignKeyDescriptor,
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			FetchStrategy mappedFetchStrategy,
			EntityMappingType type,
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess) {
		super(
				name,
				stateArrayPosition,
				attributeMetadataAccess,
				mappedFetchStrategy,
				type,
				declaringType,
				propertyAccess
		);
		this.sqlAliasStem = SqlAliasStemHelper.INSTANCE.generateStemFromAttributeName( name );

		this.foreignKeyDescriptor = foreignKeyDescriptor;
	}

	@Override
	public EntityMappingType getMappedTypeDescriptor() {
		return (EntityMappingType) super.getMappedTypeDescriptor();
	}

	@Override
	public EntityMappingType getEntityMappingType() {
		return (EntityMappingType)getMappedTypeDescriptor();
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState) {
		if ( fetchTiming == FetchTiming.IMMEDIATE && selected ) {
			final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();

			TableGroup lhsTableGroup = sqlAstCreationState.getFromClauseAccess()
					.getTableGroup( fetchParent.getNavigablePath() );

			// todo (6.0) : determine the correct JoinType
			final TableGroupJoin tableGroupJoin = createTableGroupJoin(
					fetchablePath,
					lhsTableGroup,
					null,
					JoinType.INNER,
					lockMode,
					creationState.getSqlAliasBaseManager(),
					creationState.getSqlAstCreationState().getSqlExpressionResolver(),
					creationState.getSqlAstCreationState().getCreationContext()
			);

			lhsTableGroup.addTableGroupJoin( tableGroupJoin );

			sqlAstCreationState.getFromClauseAccess().registerTableGroup(
					fetchablePath,
					tableGroupJoin.getJoinedGroup()
			);

			return new EntityFetchImpl(
					fetchParent,
					this,
					lockMode,
					!selected,
					fetchablePath,
					foreignKeyDescriptor.createDomainResult( fetchablePath, lhsTableGroup, creationState ),
					creationState
			);
		}

		return new DelayedEntityFetch(
				fetchParent,
				this,
				lockMode,
				!selected,
				fetchablePath,
				creationState
		);
	}

	@Override
	public int getNumberOfFetchables() {
		return getEntityMappingType().getNumberOfFetchables();
	}

	@Override
	public TableGroupJoin createTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			JoinType joinType,
			LockMode lockMode,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		final String aliasRoot = explicitSourceAlias == null ? sqlAliasStem : explicitSourceAlias;
		final SqlAliasBase sqlAliasBase = aliasBaseGenerator.createSqlAliasBase( aliasRoot );

		final TableGroupBuilder tableGroupBuilder = TableGroupBuilder.builder(
				navigablePath,
				this,
				lockMode,
				sqlAliasBase,
				creationContext.getSessionFactory()
		);

		applyTableReferences(
				sqlAliasBase,
				joinType,
				tableGroupBuilder,
				sqlExpressionResolver,
				creationContext
		);

		getMappedTypeDescriptor().getIdentifierMapping();

		final TableGroup tableGroup = tableGroupBuilder.build();
		final TableGroupJoin tableGroupJoin = new TableGroupJoin(
				navigablePath,
				joinType,
				tableGroup,
				null
		);

		lhs.addTableGroupJoin( tableGroupJoin );

		return tableGroupJoin;
	}

	@Override
	public String getSqlAliasStem() {
		return sqlAliasStem;
	}

	@Override
	public void applyTableReferences(
			SqlAliasBase sqlAliasBase,
			JoinType baseJoinType,
			TableReferenceCollector collector,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		getMappedTypeDescriptor().applyTableReferences(
				sqlAliasBase,
				baseJoinType,
				collector,
				sqlExpressionResolver,
				creationContext
		);
	}

}