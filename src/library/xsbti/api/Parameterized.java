package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class Parameterized extends SimpleType
{
        public Parameterized(SimpleType baseType, Type[] typeArguments)
        {
                this.baseType = baseType;
                this.typeArguments = typeArguments;
        }
        private final SimpleType baseType;
        private final Type[] typeArguments;
        public final SimpleType baseType()
        {
                return baseType;
        }
        public final Type[] typeArguments()
        {
                return typeArguments;
        }
        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof Parameterized)) {
                         return false;
                } else {
                        Parameterized o = (Parameterized)obj;
                        return baseType().equals(o.baseType()) && Arrays.deepEquals(typeArguments(), o.typeArguments());
                }
        }
        public int hashCode()
        {
                 return 37 * (37 * (17) + baseType().hashCode()) + Arrays.deepHashCode(typeArguments());
        }
        public String toString()
        {
                 return "Parameterized(" + "baseType: " + baseType() + ", " + "typeArguments: " + Arrays.toString(typeArguments()) + ")";
        }
}
