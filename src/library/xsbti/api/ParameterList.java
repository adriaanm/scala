package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class ParameterList implements java.io.Serializable
{
        public ParameterList(MethodParameter[] parameters, boolean isImplicit)
        {
                this.parameters = parameters;
                this.isImplicit = isImplicit;
        }
        private final MethodParameter[] parameters;
        private final boolean isImplicit;
        public final MethodParameter[] parameters()
        {
                return parameters;
        }
        public final boolean isImplicit()
        {
                return isImplicit;
        }
        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof ParameterList)) {
                         return false;
                } else {
                        ParameterList o = (ParameterList)obj;
                        return Arrays.deepEquals(parameters(), o.parameters()) && isImplicit() == o.isImplicit();
                }
        }
        public int hashCode()
        {
                 return 37 * (37 * (17) + Arrays.deepHashCode(parameters())) + (isImplicit() ? 0 : 1);
        }
        public String toString()
        {
                 return "ParameterList(" + "parameters: " + Arrays.toString(parameters()) + ", " + "isImplicit: " + isImplicit() + ")";
        }
}
