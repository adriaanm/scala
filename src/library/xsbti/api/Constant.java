package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class Constant extends Type
{
        public Constant(Type baseType, String value)
        {
                this.baseType = baseType;
                this.value = value;
        }
        private final Type baseType;
        private final String value;
        public final Type baseType()
        {
                return baseType;
        }
        public final String value()
        {
                return value;
        }
        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof Constant)) {
                         return false;
                } else {
                        Constant o = (Constant)obj;
                        return baseType().equals(o.baseType()) && value().equals(o.value());
                }
        }
        public int hashCode()
        {
                 return 37 * (37 * (17) + baseType().hashCode()) + value().hashCode();
        }
        public String toString()
        {
                 return "Constant(" + "baseType: " + baseType() + ", " + "value: " + value() + ")";
        }
}
