package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class TypeParameter implements java.io.Serializable
{
        public TypeParameter(String id, Annotation[] annotations, TypeParameter[] typeParameters, Variance variance, Type lowerBound, Type upperBound)
        {
                this.id = id;
                this.annotations = annotations;
                this.typeParameters = typeParameters;
                this.variance = variance;
                this.lowerBound = lowerBound;
                this.upperBound = upperBound;
        }
        private final String id;
        private final Annotation[] annotations;
        private final TypeParameter[] typeParameters;
        private final Variance variance;
        private final Type lowerBound;
        private final Type upperBound;
        public final String id()
        {
                return id;
        }
        public final Annotation[] annotations()
        {
                return annotations;
        }
        public final TypeParameter[] typeParameters()
        {
                return typeParameters;
        }
        public final Variance variance()
        {
                return variance;
        }
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
                } else if (!(obj instanceof TypeParameter)) {
                         return false;
                } else {
                        TypeParameter o = (TypeParameter)obj;
                        return id().equals(o.id()) && Arrays.deepEquals(annotations(), o.annotations()) && Arrays.deepEquals(typeParameters(), o.typeParameters()) && variance().equals(o.variance()) && lowerBound().equals(o.lowerBound()) && upperBound().equals(o.upperBound());
                }
        }
        public int hashCode()
        {
                 return 37 * (37 * (37 * (37 * (37 * (37 * (17) + id().hashCode()) + Arrays.deepHashCode(annotations())) + Arrays.deepHashCode(typeParameters())) + variance().hashCode()) + lowerBound().hashCode()) + upperBound().hashCode();
        }
        public String toString()
        {
                 return "TypeParameter(" + "id: " + id() + ", " + "annotations: " + Arrays.toString(annotations()) + ", " + "typeParameters: " + Arrays.toString(typeParameters()) + ", " + "variance: " + variance() + ", " + "lowerBound: " + lowerBound() + ", " + "upperBound: " + upperBound() + ")";
        }
}
