package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class Polymorphic extends Type
{
        public Polymorphic(Type baseType, TypeParameter[] parameters)
        {
                this.baseType = baseType;
                this.parameters = parameters;
        }
        private final Type baseType;
        private final TypeParameter[] parameters;
        public final Type baseType()
        {
                return baseType;
        }
        public final TypeParameter[] parameters()
        {
                return parameters;
        }
        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof Polymorphic)) {
                         return false;
                } else {
                        Polymorphic o = (Polymorphic)obj;
                        return baseType().equals(o.baseType()) && Arrays.deepEquals(parameters(), o.parameters());
                }
        }
        public int hashCode()
        {
                 return 37 * (37 * (17) + baseType().hashCode()) + Arrays.deepHashCode(parameters());
        }
        public String toString()
        {
                 return "Polymorphic(" + "baseType: " + baseType() + ", " + "parameters: " + Arrays.toString(parameters()) + ")";
        }
}
