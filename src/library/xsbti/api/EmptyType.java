package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class EmptyType extends SimpleType
{
        public EmptyType()
        {

        }

        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof EmptyType)) {
                         return false;
                } else {
                        EmptyType o = (EmptyType)obj;
                        return true;
                }
        }
        public int hashCode()
        {
                 return 17;
        }
        public String toString()
        {
                 return "EmptyType(" + "" + ")";
        }
}
