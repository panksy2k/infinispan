package org.infinispan.query.impl;

import java.util.HashMap;
import java.util.Map;

import org.infinispan.commands.ReplicableCommand;
import org.infinispan.commands.module.ModuleCommandFactory;
import org.infinispan.commands.remote.CacheRpcCommand;
import org.infinispan.query.clustered.SegmentsClusteredQueryCommand;
import org.infinispan.query.indexmanager.IndexUpdateCommand;
import org.infinispan.query.indexmanager.IndexUpdateStreamCommand;
import org.infinispan.util.ByteString;

/**
 * Remote commands factory implementation.
 *
 * @author Israel Lacerra &lt;israeldl@gmail.com&gt;
 * @since 5.1
 */
final class CommandFactory implements ModuleCommandFactory {

   @Override
   public Map<Byte, Class<? extends ReplicableCommand>> getModuleCommands() {
      Map<Byte, Class<? extends ReplicableCommand>> map = new HashMap<>(4);
      map.put(SegmentsClusteredQueryCommand.COMMAND_ID, SegmentsClusteredQueryCommand.class);
      map.put(IndexUpdateCommand.COMMAND_ID, IndexUpdateCommand.class);
      map.put(IndexUpdateStreamCommand.COMMAND_ID, IndexUpdateStreamCommand.class);
      return map;
   }

   @Override
   public ReplicableCommand fromStream(byte commandId) {
      // Should never be called because this factory only provides cache specific replicable commands.
      return null;
   }

   @Override
   public CacheRpcCommand fromStream(byte commandId, ByteString cacheName) {
      CacheRpcCommand c;
      switch (commandId) {
         case SegmentsClusteredQueryCommand.COMMAND_ID:
            c = new SegmentsClusteredQueryCommand(cacheName);
            break;
         case IndexUpdateCommand.COMMAND_ID:
            c = new IndexUpdateCommand(cacheName);
            break;
         case IndexUpdateStreamCommand.COMMAND_ID:
            c = new IndexUpdateStreamCommand(cacheName);
            break;
         default:
            throw new IllegalArgumentException("Not registered to handle command id " + commandId);
      }
      return c;
   }
}
