libwajava
=================

libwajava is a WhatsAPP library for Android.
Thanks goes out to the WhatsAPI team @ https://github.com/venomous0x/WhatsAPI for doing all the reverse engineering.

HowTo
=================

final WAClient client = new WAClient("YOUR_NUMBER_WITHOUT_PLUS", "PASSWORD_IN_BASE64", "DISPLAY NAME");
client.connect(new WAConnectCallback()
{

  @Override
	public void onConnect()
	{
		try
		{
			System.out.println("Connected :-)");
			client.sendMessage("SOME_NUMBER", "Hey there from libwajava!");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void onConnectFailure(Reason reason)
	{
		System.out.println("Couldn't conenct :-(");
	}
});
