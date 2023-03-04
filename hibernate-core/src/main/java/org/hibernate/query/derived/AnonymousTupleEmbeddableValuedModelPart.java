/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.derived;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.Incubating;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.persister.entity.AttributeMappingsList;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.StandardVirtualTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableResultImpl;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Christian Beikov
 */
@Incubating
public class AnonymousTupleEmbeddableValuedModelPart implements EmbeddableValuedModelPart, EmbeddableMappingType {

	private static final FetchOptions FETCH_OPTIONS = FetchOptions.valueOf( FetchTiming.IMMEDIATE, FetchStyle.JOIN );

	private final Map<String, ModelPart> modelParts;
	private final DomainType<?> domainType;
	private final String componentName;
	private final EmbeddableValuedModelPart existingModelPartContainer;
	private final int fetchableIndex;

	public AnonymousTupleEmbeddableValuedModelPart(
			Map<String, ModelPart> modelParts,
			DomainType<?> domainType,
			String componentName,
			EmbeddableValuedModelPart existingModelPartContainer,
			int fetchableIndex) {
		this.modelParts = modelParts;
		this.domainType = domainType;
		this.componentName = componentName;
		this.existingModelPartContainer = existingModelPartContainer;
		this.fetchableIndex = fetchableIndex;
	}

	@Override
	public ModelPart findSubPart(String name, EntityMappingType treatTargetType) {
		return modelParts.get( name );
	}

	@Override
	public void visitSubParts(Consumer<ModelPart> consumer, EntityMappingType treatTargetType) {
		modelParts.values().forEach( consumer );
	}

	@Override
	public MappingType getPartMappingType() {
		return this;
	}

	@Override
	public JavaType<?> getJavaType() {
		return domainType.getExpressibleJavaType();
	}

	@Override
	public String getPartName() {
		return componentName;
	}

	@Override
	public int getJdbcTypeCount() {
		return existingModelPartContainer.getJdbcTypeCount();
	}

	@Override
	public EmbeddableMappingType getEmbeddableTypeDescriptor() {
		return this;
	}

	@Override
	public EmbeddableValuedModelPart getEmbeddedValueMapping() {
		return this;
	}

	@Override
	public EmbeddableRepresentationStrategy getRepresentationStrategy() {
		return existingModelPartContainer.getEmbeddableTypeDescriptor()
				.getRepresentationStrategy();
	}

	@Override
	public boolean isCreateEmptyCompositesEnabled() {
		return false;
	}

	@Override
	public EmbeddableMappingType createInverseMappingType(
			EmbeddedAttributeMapping valueMapping,
			TableGroupProducer declaringTableGroupProducer,
			SelectableMappings selectableMappings,
			MappingModelCreationProcess creationProcess) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getNumberOfAttributeMappings() {
		return modelParts.size();
	}

	@Override
	public AttributeMapping getAttributeMapping(int position) {
		throw new UnsupportedOperationException();
	}

	@Override
	public AttributeMappingsList getAttributeMappings() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void forEachAttributeMapping(Consumer<? super AttributeMapping> action) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object[] getValues(Object instance) {
		return existingModelPartContainer.getEmbeddableTypeDescriptor()
				.getValues( instance );
	}

	@Override
	public Object getValue(Object instance, int position) {
		return existingModelPartContainer.getEmbeddableTypeDescriptor()
				.getAttributeMapping( position )
				.getValue( instance );
	}

	@Override
	public void setValues(Object instance, Object[] resolvedValues) {
		existingModelPartContainer.getEmbeddableTypeDescriptor()
						.setValues( instance, resolvedValues );
	}

	@Override
	public void setValue(Object instance, int position, Object value) {
		existingModelPartContainer.getEmbeddableTypeDescriptor()
				.getAttributeMapping( position )
				.setValue( instance, value );
	}

	@Override
	public SelectableMapping getSelectable(int columnIndex) {
		final List<SelectableMapping> results = new ArrayList<>();
		forEachSelectable( (index, selection) -> results.add( selection ) );
		return results.get( columnIndex );
	}

	@Override
	public List<JdbcMapping> getJdbcMappings() {
		final List<JdbcMapping> results = new ArrayList<>();
		forEachSelectable( (index, selection) -> results.add( selection.getJdbcMapping() ) );
		return results;
	}

	@Override
	public int forEachSelectable(SelectableConsumer consumer) {
		return forEachSelectable( 0, consumer );
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		int span = 0;
		for ( ModelPart mapping : modelParts.values() ) {
			span += mapping.forEachSelectable( offset + span, consumer );
		}
		return span;
	}

	@Override
	public String getContainingTableExpression() {
		return "";
	}

	@Override
	public SqlTuple toSqlExpression(
			TableGroup tableGroup,
			Clause clause,
			SqmToSqlAstConverter walker,
			SqlAstCreationState sqlAstCreationState) {
		final List<ColumnReference> columnReferences = CollectionHelper.arrayList( getJdbcTypeCount() );
		final NavigablePath navigablePath = tableGroup.getNavigablePath().append( componentName );
		final TableReference tableReference = tableGroup.resolveTableReference( navigablePath, getContainingTableExpression() );
		for ( ModelPart modelPart : modelParts.values() ) {
			modelPart.forEachSelectable(
					(columnIndex, selection) -> {
						final Expression columnReference = sqlAstCreationState.getSqlExpressionResolver()
								.resolveSqlExpression( tableReference, selection );

						columnReferences.add( columnReference.getColumnReference() );
					}
			);
		}

		return new SqlTuple( columnReferences, this );
	}

	@Override
	public JavaType<?> getMappedJavaType() {
		return existingModelPartContainer.getJavaType();
	}

	@Override
	public SqlAstJoinType getDefaultSqlAstJoinType(TableGroup parentTableGroup) {
		return SqlAstJoinType.INNER;
	}

	@Override
	public boolean isSimpleJoinPredicate(Predicate predicate) {
		return predicate == null;
	}

	@Override
	public TableGroupJoin createTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			SqlAliasBase explicitSqlAliasBase,
			SqlAstJoinType requestedJoinType,
			boolean fetched,
			boolean addsPredicate,
			SqlAstCreationState creationState) {
		final SqlAstJoinType joinType = requestedJoinType == null ? SqlAstJoinType.INNER : requestedJoinType;
		final TableGroup tableGroup = createRootTableGroupJoin(
				navigablePath,
				lhs,
				explicitSourceAlias,
				explicitSqlAliasBase,
				requestedJoinType,
				fetched,
				null,
				creationState
		);

		return new TableGroupJoin( navigablePath, joinType, tableGroup );
	}

	@Override
	public TableGroup createRootTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			SqlAliasBase explicitSqlAliasBase,
			SqlAstJoinType sqlAstJoinType,
			boolean fetched,
			Consumer<Predicate> predicateConsumer,
			SqlAstCreationState creationState) {
		return new StandardVirtualTableGroup(
				navigablePath,
				this,
				lhs,
				fetched
		);
	}

	@Override
	public String getSqlAliasStem() {
		return getPartName();
	}

	@Override
	public String getFetchableName() {
		return getPartName();
	}

	@Override
	public int getFetchableKey() {
		return fetchableIndex;
	}

	@Override
	public FetchOptions getMappedFetchOptions() {
		return FETCH_OPTIONS;
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		throw new UnsupportedOperationException( "AnonymousTupleEmbeddableValuedModelPart is not fetchable" );
	}

	@Override
	public int getNumberOfFetchables() {
		return modelParts.size();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return null;
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return null;
	}

	@Override
	public boolean hasPartitionedSelectionMapping() {
		return false;
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		return new EmbeddableResultImpl<>(
				navigablePath,
				this,
				resultVariable,
				creationState
		);
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		for ( ModelPart mapping : modelParts.values() ) {
			mapping.applySqlSelections( navigablePath, tableGroup, creationState );
		}
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		for ( ModelPart mapping : modelParts.values() ) {
			mapping.applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
		}
	}

	@Override
	public <X, Y> int breakDownJdbcValues(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
		final Object[] values = (Object[]) domainValue;
		assert values.length == modelParts.size();
		int span = 0;
		int i = 0;
		for ( ModelPart mapping : modelParts.values() ) {
			final Object attributeValue = values[ i ];
			span += mapping.breakDownJdbcValues( attributeValue, offset + span, x, y, valueConsumer, session );
			i++;
		}
		return span;
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		final Object[] values = (Object[]) value;
		final Object[] result = new Object[ modelParts.size() ];
		int i = 0;
		for ( ModelPart mapping : modelParts.values() ) {
			Object o = values[i];
			result[i] = mapping.disassemble( o, session );
			i++;
		}

		return result;
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		final Object[] values = (Object[]) value;
		int span = 0;
		int i = 0;
		for ( ModelPart mapping : modelParts.values() ) {
			span += mapping.forEachDisassembledJdbcValue( values[i], span + offset, x, y, valuesConsumer, session );
			i++;
		}
		return span;
	}

	@Override
	public <X, Y> int forEachJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> consumer,
			SharedSessionContractImplementor session) {
		final Object[] values = (Object[]) value;
		int span = 0;
		int i = 0;
		for ( ModelPart attributeMapping : modelParts.values() ) {
			final Object o = values[i];
			span += attributeMapping.forEachJdbcValue( o, span + offset, x, y, consumer, session );
			i++;
		}
		return span;
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		int span = 0;
		for ( ModelPart attributeMapping : modelParts.values() ) {
			span += attributeMapping.forEachJdbcType( span + offset, action );
		}
		return span;
	}
}
