package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class TypeDeclaration extends TypeMember
{
        public TypeDeclaration(Type lowerBound, Type upperBound, TypeParameter[] typeParameters, String name, Access access, Modifiers modifiers, Annotation[] annotations)
        {
                super(typeParameters, name, access, modifiers, annotations);
                this.lowerBound = lowerBound;
                this.upperBound = upperBound;
        }
        private final Type lowerBound;
        private final Type upperBound;
        public final Type lowerBound()
        {
                return lowerBound;
        }
        public final Type upperBound()
        {
                return upperBound;
        }
        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof TypeDeclaration)) {
                         return false;
                } else {
                        TypeDeclaration o = (TypeDeclaration)obj;
                        return lowerBound().equals(o.lowerBound()) && upperBound().equals(o.upperBound()) && Arrays.deepEquals(typeParameters(), o.typeParameters()) && name().equals(o.name()) && access().equals(o.access()) && modifiers().equals(o.modifiers()) && Arrays.deepEquals(annotations(), o.annotations());
                }
        }
        public int hashCode()
        {
                 return 37 * (37 * (37 * (37 * (37 * (37 * (37 * (17) + lowerBound().hashCode()) + upperBound().hashCode()) + Arrays.deepHashCode(typeParameters())) + name().hashCode()) + access().hashCode()) + modifiers().hashCode()) + Arrays.deepHashCode(annotations());
        }
        public String toString()
        {
                 return "TypeDeclaration(" + "lowerBound: " + lowerBound() + ", " + "upperBound: " + upperBound() + ", " + "typeParameters: " + Arrays.toString(typeParameters()) + ", " + "name: " + name() + ", " + "access: " + access() + ", " + "modifiers: " + modifiers() + ", " + "annotations: " + Arrays.toString(annotations()) + ")";
        }
}
