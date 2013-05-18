/**
 * 
 */
package de.michaelfuerst.hangout;

import java.io.FileDescriptor;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * @author Michael
 *
 */
public class HIBinder implements IBinder {
	private HangoutNetwork networkAdaper = null;
	
	public HIBinder(HangoutNetwork networkAdapter) {
		this.networkAdaper = networkAdapter;
	}
	
	public HangoutNetwork getNetworkAdapter() {
		return networkAdaper;
	}

	/* (non-Javadoc)
	 * @see android.os.IBinder#dump(java.io.FileDescriptor, java.lang.String[])
	 */
	@Override
	public void dump(FileDescriptor fd, String[] args) throws RemoteException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see android.os.IBinder#dumpAsync(java.io.FileDescriptor, java.lang.String[])
	 */
	@Override
	public void dumpAsync(FileDescriptor fd, String[] args)
			throws RemoteException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see android.os.IBinder#getInterfaceDescriptor()
	 */
	@Override
	public String getInterfaceDescriptor() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see android.os.IBinder#isBinderAlive()
	 */
	@Override
	public boolean isBinderAlive() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see android.os.IBinder#linkToDeath(android.os.IBinder.DeathRecipient, int)
	 */
	@Override
	public void linkToDeath(DeathRecipient recipient, int flags)
			throws RemoteException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see android.os.IBinder#pingBinder()
	 */
	@Override
	public boolean pingBinder() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see android.os.IBinder#queryLocalInterface(java.lang.String)
	 */
	@Override
	public IInterface queryLocalInterface(String descriptor) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see android.os.IBinder#transact(int, android.os.Parcel, android.os.Parcel, int)
	 */
	@Override
	public boolean transact(int code, Parcel data, Parcel reply, int flags)
			throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see android.os.IBinder#unlinkToDeath(android.os.IBinder.DeathRecipient, int)
	 */
	@Override
	public boolean unlinkToDeath(DeathRecipient recipient, int flags) {
		// TODO Auto-generated method stub
		return false;
	}

}
