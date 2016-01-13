package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class Source implements java.io.Serializable
{
        public Source(Compilation compilation, byte[] hash, SourceAPI api, int apiHash, _internalOnly_NameHashes _internalOnly_nameHashes, boolean hasMacro)
        {
                this.compilation = compilation;
                this.hash = hash;
                this.api = api;
                this.apiHash = apiHash;
                this._internalOnly_nameHashes = _internalOnly_nameHashes;
                this.hasMacro = hasMacro;
        }
        private final Compilation compilation;
        private final byte[] hash;
        private final SourceAPI api;
        private final int apiHash;
        private final _internalOnly_NameHashes _internalOnly_nameHashes;
        private final boolean hasMacro;
        public final Compilation compilation()
        {
                return compilation;
        }
        public final byte[] hash()
        {
                return hash;
        }
        public final SourceAPI api()
        {
                return api;
        }
        public final int apiHash()
        {
                return apiHash;
        }
        public final _internalOnly_NameHashes _internalOnly_nameHashes()
        {
                return _internalOnly_nameHashes;
        }
        public final boolean hasMacro()
        {
                return hasMacro;
        }
        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof Source)) {
                         return false;
                } else {
                        Source o = (Source)obj;
                        return compilation().equals(o.compilation()) && Arrays.equals(hash(), o.hash()) && api().equals(o.api()) && apiHash() == o.apiHash() && _internalOnly_nameHashes().equals(o._internalOnly_nameHashes()) && hasMacro() == o.hasMacro();
                }
        }
        public int hashCode()
        {
                 return 37 * (37 * (37 * (37 * (37 * (37 * (17) + compilation().hashCode()) + Arrays.hashCode(hash())) + api().hashCode()) + apiHash()) + _internalOnly_nameHashes().hashCode()) + (hasMacro() ? 0 : 1);
        }
        public String toString()
        {
                 return "Source(" + "compilation: " + compilation() + ", " + "hash: " + Arrays.toString(hash()) + ", " + "api: " + api() + ", " + "apiHash: " + apiHash() + ", " + "_internalOnly_nameHashes: " + _internalOnly_nameHashes() + ", " + "hasMacro: " + hasMacro() + ")";
        }
}
