/**
 * @author jef@cs.nott.ac.uk 
 */

package mrl.automics.sensors;

import mrl.automics.sensors.IRemoteServiceCallback;

/**
 *  An interface for calling on to a remote service
 * (running in another process).
 */
interface IRemoteService {
    /**
     * Allow a service to call back to its clients 
     * by registering a callback interface with
     * the service.
     */
    void registerCallback(IRemoteServiceCallback cb);
    
    /**
     * Remove a previously registered callback interface.
     */
    void unregisterCallback(IRemoteServiceCallback cb);
}
