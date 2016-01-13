package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class ClassLike extends ParameterizedDefinition
{
        public ClassLike(DefinitionType definitionType, Lazy<Type> selfType, Lazy<Structure> structure, String[] savedAnnotations, TypeParameter[] typeParameters, String name, Access access, Modifiers modifiers, Annotation[] annotations)
        {
                super(typeParameters, name, access, modifiers, annotations);
                this.definitionType = definitionType;
                this.selfType = selfType;
                this.structure = structure;
                this.savedAnnotations = savedAnnotations;
        }
        private final DefinitionType definitionType;
        private final Lazy<Type> selfType;
        private final Lazy<Structure> structure;
        private final String[] savedAnnotations;
        public final DefinitionType definitionType()
        {
                return definitionType;
        }
        public final Type selfType()
        {
                return selfType.get();
        }
        public final Structure structure()
        {
                return structure.get();
        }
        public final String[] savedAnnotations()
        {
                return savedAnnotations;
        }
        public boolean equals(Object obj)
        {
                 return this == obj;  // We have lazy members, so use object identity to avoid circularity.
        }
        public int hashCode()
        {
                 return super.hashCode();
        }
        public String toString()
        {
                 return super.toString();
        }
}
