package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class Existential extends Type
{
        public Existential(Type baseType, TypeParameter[] clause)
        {
                this.baseType = baseType;
                this.clause = clause;
        }
        private final Type baseType;
        private final TypeParameter[] clause;
        public final Type baseType()
        {
                return baseType;
        }
        public final TypeParameter[] clause()
        {
                return clause;
        }
        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof Existential)) {
                         return false;
                } else {
                        Existential o = (Existential)obj;
                        return baseType().equals(o.baseType()) && Arrays.deepEquals(clause(), o.clause());
                }
        }
        public int hashCode()
        {
                 return 37 * (37 * (17) + baseType().hashCode()) + Arrays.deepHashCode(clause());
        }
        public String toString()
        {
                 return "Existential(" + "baseType: " + baseType() + ", " + "clause: " + Arrays.toString(clause()) + ")";
        }
}
