package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class Public extends Access
{
        public Public()
        {

        }

        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof Public)) {
                         return false;
                } else {
                        Public o = (Public)obj;
                        return true;
                }
        }
        public int hashCode()
        {
                 return 17;
        }
        public String toString()
        {
                 return "Public(" + "" + ")";
        }
}
