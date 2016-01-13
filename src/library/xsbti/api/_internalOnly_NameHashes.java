package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class _internalOnly_NameHashes implements java.io.Serializable
{
        public _internalOnly_NameHashes(_internalOnly_NameHash[] regularMembers, _internalOnly_NameHash[] implicitMembers)
        {
                this.regularMembers = regularMembers;
                this.implicitMembers = implicitMembers;
        }
        private final _internalOnly_NameHash[] regularMembers;
        private final _internalOnly_NameHash[] implicitMembers;
        public final _internalOnly_NameHash[] regularMembers()
        {
                return regularMembers;
        }
        public final _internalOnly_NameHash[] implicitMembers()
        {
                return implicitMembers;
        }
        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof _internalOnly_NameHashes)) {
                         return false;
                } else {
                        _internalOnly_NameHashes o = (_internalOnly_NameHashes)obj;
                        return Arrays.deepEquals(regularMembers(), o.regularMembers()) && Arrays.deepEquals(implicitMembers(), o.implicitMembers());
                }
        }
        public int hashCode()
        {
                 return 37 * (37 * (17) + Arrays.deepHashCode(regularMembers())) + Arrays.deepHashCode(implicitMembers());
        }
        public String toString()
        {
                 return "_internalOnly_NameHashes(" + "regularMembers: " + Arrays.toString(regularMembers()) + ", " + "implicitMembers: " + Arrays.toString(implicitMembers()) + ")";
        }
}
