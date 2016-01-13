package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class ThisQualifier extends Qualifier
{
        public ThisQualifier()
        {

        }

        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof ThisQualifier)) {
                         return false;
                } else {
                        ThisQualifier o = (ThisQualifier)obj;
                        return true;
                }
        }
        public int hashCode()
        {
                 return 17;
        }
        public String toString()
        {
                 return "ThisQualifier(" + "" + ")";
        }
}
