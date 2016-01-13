package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class Annotated extends Type
{
        public Annotated(Type baseType, Annotation[] annotations)
        {
                this.baseType = baseType;
                this.annotations = annotations;
        }
        private final Type baseType;
        private final Annotation[] annotations;
        public final Type baseType()
        {
                return baseType;
        }
        public final Annotation[] annotations()
        {
                return annotations;
        }
        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof Annotated)) {
                         return false;
                } else {
                        Annotated o = (Annotated)obj;
                        return baseType().equals(o.baseType()) && Arrays.deepEquals(annotations(), o.annotations());
                }
        }
        public int hashCode()
        {
                 return 37 * (37 * (17) + baseType().hashCode()) + Arrays.deepHashCode(annotations());
        }
        public String toString()
        {
                 return "Annotated(" + "baseType: " + baseType() + ", " + "annotations: " + Arrays.toString(annotations()) + ")";
        }
}
