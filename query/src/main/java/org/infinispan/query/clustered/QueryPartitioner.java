package org.infinispan.query.clustered;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.commons.util.IntSet;
import org.infinispan.configuration.cache.ClusteringConfiguration;
import org.infinispan.configuration.cache.Index;
import org.infinispan.distribution.LocalizedCacheTopology;
import org.infinispan.query.impl.IndexInspector;
import org.infinispan.remoting.transport.Address;

/**
 * Partition segments across cluster members to satisfy a query
 *
 * @since 10.1
 */
final class QueryPartitioner {
   private final Cache<?, ?> cache;
   private final IndexInspector indexInspector;
   private final int numSegments;
   private final Index indexMode;

   public QueryPartitioner(Cache<?, ?> cache, IndexInspector indexInspector) {
      this.cache = cache;
      ClusteringConfiguration clustering = cache.getCacheConfiguration().clustering();
      this.numSegments = clustering.hash().numSegments();
      this.indexInspector = indexInspector;
      this.indexMode = cache.getCacheConfiguration().indexing().index();
   }

   private boolean hasSharedIndex(Class<?> entity) {
      if (indexMode != null) {
         return Index.PRIMARY_OWNER == indexMode;
      }
      return indexInspector.hasSharedIndex(entity);
   }

   public Map<Address, BitSet> split(Class<?> targetEntity) {
      AdvancedCache<?, ?> advancedCache = cache.getAdvancedCache();
      List<Address> members = advancedCache.getRpcManager().getMembers();
      Address localAddress = advancedCache.getRpcManager().getAddress();
      LocalizedCacheTopology cacheTopology = advancedCache.getDistributionManager().getCacheTopology();
      BitSet bitSet = new BitSet();
      Map<Address, BitSet> segmentsPerMember = new LinkedHashMap<>(members.size());

      if (!hasSharedIndex(targetEntity)) {
         IntSet localSegments = cacheTopology.getLocalReadSegments();
         localSegments.stream().forEach(bitSet::set);
         segmentsPerMember.put(localAddress, bitSet);
         for (int s = 0; s < numSegments; s++) {
            if (!localSegments.contains(s)) {
               Address primary = cacheTopology.getSegmentDistribution(s).primary();
               segmentsPerMember.computeIfAbsent(primary, address -> new BitSet()).set(s);
            }
         }
      } else {
         for (int s = 0; s < numSegments; s++) {
            Address primary = cacheTopology.getSegmentDistribution(s).primary();
            segmentsPerMember.computeIfAbsent(primary, address -> new BitSet()).set(s);
         }
      }

      return segmentsPerMember;
   }


}
